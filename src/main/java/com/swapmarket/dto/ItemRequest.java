package com.swapmarket.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemRequest {
    @NotBlank
    private String title;
    private String description;
    private String category;
    private String size;
    private String condition;

    // Optional original retail price in INR; drives the estimated swap value.
    @DecimalMin(value = "0.0", inclusive = false, message = "Original price must be greater than zero")
    @DecimalMax(value = "1000000", message = "Original price looks too high")
    private BigDecimal originalPrice;

    private List<String> images;
}
