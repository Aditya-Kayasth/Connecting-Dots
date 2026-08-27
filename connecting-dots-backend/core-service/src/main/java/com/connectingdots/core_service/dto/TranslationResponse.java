package com.connectingdots.core_service.dto;

public record TranslationResponse(
        String translatedTitle,
        String translatedDescription,
        String targetLanguage
) {}
