package com.connectingdots.ai_service.dto;

public record TranslationRequest(
        String title,
        String description,
        String targetLanguage
) {}
