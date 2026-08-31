package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContributorProfileRequest(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        String skillsSummary,
        String portfolioUrl,
        String preferredLanguage,
        String title,
        String location,
        @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Please enter a valid phone number (7 to 15 digits)")
        String contactNumber
) {
    public ContributorProfileRequest(String firstName, String lastName, String skillsSummary, String portfolioUrl) {
        this(firstName, lastName, skillsSummary, portfolioUrl, "en", "Technical Contributor", "Community Member", "");
    }
}