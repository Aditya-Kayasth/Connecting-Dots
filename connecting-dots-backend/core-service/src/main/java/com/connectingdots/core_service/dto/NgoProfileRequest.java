package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;

public record NgoProfileRequest(
        @NotBlank(message = "Organization name is required")
        String organizationName,
        @NotBlank(message = "Domain is required")
        String domain,
        String contactNumber,
        String preferredLanguage
) {
    public NgoProfileRequest(String organizationName, String domain, String contactNumber) {
        this(organizationName, domain, contactNumber, "en");
    }
}