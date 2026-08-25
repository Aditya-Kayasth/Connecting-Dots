package com.connectingdots.core_service.dto;

public record CloudinarySignatureResponse(
    String signature,
    long timestamp,
    String apiKey,
    String cloudName,
    String folder
) {
}