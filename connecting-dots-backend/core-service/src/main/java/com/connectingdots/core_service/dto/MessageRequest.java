package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
    @NotBlank(message = "Message content cannot be blank")
    String content
) {}
