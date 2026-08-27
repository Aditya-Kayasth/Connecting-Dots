package com.connectingdots.core_service.dto;

public record TranslationRequest(
        String title,
        String description,
        String targetLanguage
) {}
