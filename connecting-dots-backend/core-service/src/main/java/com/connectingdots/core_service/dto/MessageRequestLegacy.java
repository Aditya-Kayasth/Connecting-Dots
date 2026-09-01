package com.connectingdots.core_service.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MessageRequestLegacy(
    @NotNull(message = "applicationId is required")
    UUID applicationId,

    @NotBlank(message = "Message content cannot be blank")
    String message
) {}
