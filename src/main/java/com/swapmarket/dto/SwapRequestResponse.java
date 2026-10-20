package com.swapmarket.dto;

import com.swapmarket.enums.SwapStatus;

import java.time.Instant;

public record SwapRequestResponse(
        Long id,
        SwapStatus status,
        ItemResponse item,
        ItemResponse offeredItem,   // null for deposit-only swaps
        UserSummary requester,
        PaymentSummary payment,     // null until a deposit has been created
        ValueComparison valueComparison,
        String rejectionReason,     // only set when the owner rejected the request with a note
        Instant createdAt) {}
