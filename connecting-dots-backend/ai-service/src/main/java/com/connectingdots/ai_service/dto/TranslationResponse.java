package com.connectingdots.ai_service.dto;

public record TranslationResponse(
        String translatedTitle,
        String translatedDescription,
        String targetLanguage
) {}
