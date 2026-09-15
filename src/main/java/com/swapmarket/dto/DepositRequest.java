package com.swapmarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    private Long swapRequestId;
    private BigDecimal amount;
}
