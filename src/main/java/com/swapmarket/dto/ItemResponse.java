package com.swapmarket.dto;

import com.swapmarket.enums.ItemStatus;

import java.time.Instant;
import java.util.List;

public record ItemResponse(
        Long id,
        String title,
        String description,
        String category,
        String size,
        String condition,
        List<String> images,
        ItemStatus status,
        UserSummary owner,
        Instant createdAt) {}
