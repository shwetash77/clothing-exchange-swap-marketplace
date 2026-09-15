package com.swapmarket.dto;

import lombok.Data;

@Data
public class SwapRequestDto {
    private Long itemId;          // item being requested
    private Long offeredItemId;   // optional - null means deposit-only swap
}
