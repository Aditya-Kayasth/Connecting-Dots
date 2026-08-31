package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContributorProfileUpdateRequest(
    @NotBlank(message = "firstName is required")
    String firstName,

    @NotBlank(message = "lastName is required")
    String lastName,

    String skillsSummary,
    String portfolioUrl,
    String preferredLanguage,
    String title,
    String location,
    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Please enter a valid phone number (7 to 15 digits)")
    String contactNumber
) {}
