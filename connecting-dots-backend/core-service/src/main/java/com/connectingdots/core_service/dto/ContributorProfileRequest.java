package com.connectingdots.core_service.dto;

public record ContributorProfileRequest(
        String firstName,
        String lastName,
        String skillsSummary,
        String portfolioUrl,
        String preferredLanguage
) {
    public ContributorProfileRequest(String firstName, String lastName, String skillsSummary, String portfolioUrl) {
        this(firstName, lastName, skillsSummary, portfolioUrl, "en");
    }
}