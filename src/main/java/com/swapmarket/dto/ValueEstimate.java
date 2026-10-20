package com.swapmarket.dto;

import java.math.BigDecimal;

/** Result of the swap value calculator for a given original price and condition (INR). */
public record ValueEstimate(BigDecimal estimatedValue) {}
