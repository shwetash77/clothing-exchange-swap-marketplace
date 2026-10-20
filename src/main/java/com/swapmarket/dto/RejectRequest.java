package com.swapmarket.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectRequest {
    // Optional note shown to the requester.
    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
