package com.connectingdots.core_service.dto;

public record AiUpdatePayload(
        String title,
        String description,
        String domain,
        String status
) {}
