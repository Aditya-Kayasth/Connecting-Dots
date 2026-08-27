package com.connectingdots.core_service.dto;

public record NgoProfileRequest(
        String organizationName,
        String domain,
        String contactNumber,
        String preferredLanguage
) {
    public NgoProfileRequest(String organizationName, String domain, String contactNumber) {
        this(organizationName, domain, contactNumber, "en");
    }
}