package com.swapmarket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ItemRequest {
    @NotBlank
    private String title;
    private String description;
    private String category;
    private String size;
    private String condition;
    private List<String> images;
}
