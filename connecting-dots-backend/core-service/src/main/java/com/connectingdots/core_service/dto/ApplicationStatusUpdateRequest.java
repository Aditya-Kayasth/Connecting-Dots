package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplicationStatusUpdateRequest(
    @NotBlank(message = "Status cannot be blank")
    String status
) {}
