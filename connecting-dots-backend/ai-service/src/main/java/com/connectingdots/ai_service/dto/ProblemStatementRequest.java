package com.connectingdots.ai_service.dto;

public record ProblemStatementRequest(
        String title,
        String description,
        String domain
) {}