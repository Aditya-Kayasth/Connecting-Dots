package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ContributorProfileRequest(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        String skillsSummary,
        String portfolioUrl,
        String preferredLanguage
) {
    public ContributorProfileRequest(String firstName, String lastName, String skillsSummary, String portfolioUrl) {
        this(firstName, lastName, skillsSummary, portfolioUrl, "en");
    }
}