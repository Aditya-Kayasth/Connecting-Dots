package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewRequest(
        @NotNull(message = "problemId is required")
        UUID problemId,

        @NotNull(message = "rateeId is required")
        UUID rateeId,

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        int rating,

        String comment
) {}
