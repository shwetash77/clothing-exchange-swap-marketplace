package com.swapmarket.dto;

import com.swapmarket.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemResponse(
        Long id,
        String title,
        String description,
        String category,
        String size,
        String condition,
        BigDecimal originalPrice,   // null if the owner did not enter one
        BigDecimal estimatedValue,  // swap value calculator result, null if no original price
        List<String> images,
        ItemStatus status,
        UserSummary owner,
        Instant createdAt) {}
