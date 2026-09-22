package com.swapmarket.dto;

/** Public view of a user - never exposes email or password hash. */
public record UserSummary(Long id, String name) {}
