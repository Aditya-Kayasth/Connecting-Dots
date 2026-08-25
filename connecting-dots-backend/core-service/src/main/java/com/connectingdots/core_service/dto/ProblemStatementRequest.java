package com.connectingdots.core_service.dto;

public record ProblemStatementRequest(
        String title,
        String description,
        String domain,
        String sourceFileUrl,
        String sourceType,
        String status
) {}