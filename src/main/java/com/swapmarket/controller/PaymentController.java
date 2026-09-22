package com.swapmarket.controller;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.swapmarket.dto.DepositRequest;
import com.swapmarket.dto.PaymentSummary;
import com.swapmarket.mapper.ResponseMapper;
import com.swapmarket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ResponseMapper mapper;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/deposit")
    public ResponseEntity<PaymentSummary> createDeposit(@RequestBody DepositRequest request) throws RazorpayException {
        return ResponseEntity.ok(mapper.toPayment(paymentService.createDeposit(request)));
    }

    @PostMapping("/{swapRequestId}/release")
    public ResponseEntity<Void> release(@PathVariable Long swapRequestId) throws RazorpayException {
        paymentService.releaseDeposit(swapRequestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{swapRequestId}/refund")
    public ResponseEntity<Void> refund(@PathVariable Long swapRequestId) throws RazorpayException {
        paymentService.refundDeposit(swapRequestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Razorpay webhook receiver. Two important details interviewers will
     * probe on:
     * 1. Signature verification (Utils.verifyWebhookSignature) - without
     *    this, anyone could POST a fake "payment authorized" event and
     *    unlock items for free. Razorpay signs every webhook with HMAC
     *    SHA256 using the webhook secret you set in their dashboard.
     * 2. This endpoint is explicitly permitAll() in SecurityConfig -
     *    Razorpay's servers can't attach our JWT, so signature
     *    verification IS the authentication for this route.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event");

        if ("payment.authorized".equals(eventType)) {
            JSONObject paymentEntity = event.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String paymentId = paymentEntity.getString("id");
            String orderId = paymentEntity.getString("order_id");

            paymentService.handlePaymentAuthorized(orderId, paymentId);
        }

        return ResponseEntity.ok("received");
    }
}
