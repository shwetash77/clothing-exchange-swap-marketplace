package com.swapmarket.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.swapmarket.dto.DepositRequest;
import com.swapmarket.entity.SwapRequest;
import com.swapmarket.entity.SwapTransaction;
import com.swapmarket.enums.TransactionStatus;
import com.swapmarket.exception.ApiException;
import com.swapmarket.repository.SwapRequestRepository;
import com.swapmarket.repository.SwapTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SwapTransactionRepository transactionRepository;
    private final SwapRequestRepository swapRequestRepository;
    private final SwapService swapService;
    private final RazorpayClient razorpayClient;

    /**
     * Step 1 of escrow: create a Razorpay Order with payment_capture = 0.
     * This means when the user pays against this order, funds are
     * AUTHORIZED (held) but not yet captured/settled - the defining
     * property of an escrow-style flow. We only capture (release) or
     * cancel/refund once both sides confirm the physical swap happened.
     *
     * Note the flow shape here is slightly different from a pure backend
     * charge: the user actually completes payment client-side (via
     * Razorpay Checkout, using a test card in test mode) against this
     * Order id. This method just creates the Order; handlePaymentAuthorized
     * records the resulting payment once it comes in via webhook.
     */
    @Transactional
    public SwapTransaction createDeposit(DepositRequest dto) throws RazorpayException {
        SwapRequest swapRequest = swapRequestRepository.findById(dto.getSwapRequestId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap request not found"));

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", dto.getAmount().multiply(BigDecimal.valueOf(100)).longValue()); // paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "swap_" + dto.getSwapRequestId());
        orderRequest.put("payment_capture", 0); // manual capture = escrow hold

        com.razorpay.Order order = razorpayClient.orders.create(orderRequest);

        SwapTransaction transaction = new SwapTransaction();
        transaction.setSwapRequest(swapRequest);
        transaction.setDepositAmount(dto.getAmount());
        transaction.setRazorpayOrderId(order.get("id"));
        transaction.setStatus(TransactionStatus.CREATED);

        return transactionRepository.save(transaction);
    }

    /**
     * Step 2: Razorpay webhook confirms the user's payment was authorized
     * against our order (event: payment.authorized).
     *
     * Idempotency note: Razorpay explicitly documents webhooks can be
     * delivered more than once for the same event. We guard against
     * double-processing by looking up the transaction via the Order id
     * (unique in our schema) and checking current status before
     * transitioning - if it's already HELD, we no-op instead of
     * re-running side effects.
     */
    @Transactional
    public void handlePaymentAuthorized(String razorpayOrderId, String razorpayPaymentId) {
        SwapTransaction transaction = transactionRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found for order"));

        if (transaction.getStatus() == TransactionStatus.HELD) {
            return; // already processed - webhook replay, safe no-op
        }

        transaction.setRazorpayPaymentId(razorpayPaymentId);
        transaction.setStatus(TransactionStatus.HELD);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);
    }

    /**
     * Step 3: both parties confirm the physical swap happened. Captures
     * the held payment. In a real deployment you'd typically capture only
     * a small platform fee and refund the rest of the deposit -
     * simplified here to a full capture for clarity.
     */
    @Transactional
    public void releaseDeposit(Long swapRequestId) throws RazorpayException {
        SwapTransaction transaction = transactionRepository.findBySwapRequestId(swapRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.HELD) {
            throw new ApiException(HttpStatus.CONFLICT, "Deposit is not in a held state");
        }

        JSONObject captureRequest = new JSONObject();
        captureRequest.put("amount", transaction.getDepositAmount().multiply(BigDecimal.valueOf(100)).longValue());
        captureRequest.put("currency", "INR");

        razorpayClient.payments.capture(transaction.getRazorpayPaymentId(), captureRequest);

        transaction.setStatus(TransactionStatus.RELEASED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        swapService.completeSwap(swapRequestId);
    }

    /**
     * Cancellation/dispute path: refund the authorized-but-not-captured
     * payment (a true no-charge reversal on the user's card) and release
     * the item lock so it goes back on the marketplace.
     */
    @Transactional
    public void refundDeposit(Long swapRequestId) throws RazorpayException {
        SwapTransaction transaction = transactionRepository.findBySwapRequestId(swapRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", transaction.getDepositAmount().multiply(BigDecimal.valueOf(100)).longValue());

        razorpayClient.payments.refund(transaction.getRazorpayPaymentId(), refundRequest);

        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        swapService.cancelSwap(swapRequestId);
    }
}
