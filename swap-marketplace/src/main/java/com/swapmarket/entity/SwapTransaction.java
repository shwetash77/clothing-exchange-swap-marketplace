package com.swapmarket.entity;

import com.swapmarket.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "swap_transactions")
@Getter
@Setter
@NoArgsConstructor
public class SwapTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_request_id", nullable = false, unique = true)
    private SwapRequest swapRequest;

    @Column(nullable = false)
    private BigDecimal depositAmount;

    /**
     * Razorpay Order id, created with payment_capture=0 (manual capture) -
     * this is what makes the deposit an escrow hold rather than an
     * immediate charge. Used as our idempotency anchor: when a webhook
     * event arrives, we look up the transaction by this id rather than
     * trusting the webhook payload alone, since Razorpay (like every
     * payment provider) can redeliver the same webhook event more than once.
     */
    @Column(unique = true)
    private String razorpayOrderId;

    /**
     * Set once the user actually pays against the order (authorized, held).
     * This is the id used to capture or refund the payment later.
     */
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.CREATED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
