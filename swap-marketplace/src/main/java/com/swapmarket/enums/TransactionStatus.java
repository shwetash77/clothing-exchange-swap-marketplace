package com.swapmarket.enums;

public enum TransactionStatus {
    CREATED,    // PaymentIntent created, awaiting confirmation
    HELD,       // funds authorized/held (escrow)
    RELEASED,   // swap completed successfully, deposit released/captured
    REFUNDED,   // swap cancelled or disputed, deposit refunded
    FAILED
}
