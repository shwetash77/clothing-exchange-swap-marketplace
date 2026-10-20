package com.swapmarket.dto;

import com.swapmarket.enums.ValueVerdict;

import java.math.BigDecimal;

/**
 * Side-by-side value of the requested item and the offered item (all INR).
 *
 * @param requestedValue estimated value of the item being requested, null if unknown
 * @param offeredValue   estimated value of the offered item, null if none offered or unknown
 * @param difference     offeredValue - requestedValue (negative = offer is worth less), null if not comparable
 * @param suggestedTopUp amount that would even things out when the offer is lower, otherwise null
 * @param verdict        FAIR / OFFER_LOWER / OFFER_HIGHER / DEPOSIT_ONLY / UNKNOWN
 */
public record ValueComparison(
        BigDecimal requestedValue,
        BigDecimal offeredValue,
        BigDecimal difference,
        BigDecimal suggestedTopUp,
        ValueVerdict verdict) {}
