package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record ReviewRequest(
        UUID problemId,
        UUID rateeId,
        UUID applicationId, // Added to support frontend's payload

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        int rating,

        String comment
) {}
