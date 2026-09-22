package com.swapmarket.dto;

import com.swapmarket.enums.TransactionStatus;

import java.math.BigDecimal;

/** What the frontend needs to show escrow state and (re)open Razorpay Checkout. */
public record PaymentSummary(TransactionStatus status, BigDecimal amount, String orderId) {}
