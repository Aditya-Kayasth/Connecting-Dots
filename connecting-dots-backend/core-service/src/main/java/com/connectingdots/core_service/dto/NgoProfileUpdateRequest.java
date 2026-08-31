package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NgoProfileUpdateRequest(
    @NotBlank(message = "organizationName is required")
    String organizationName,

    @NotBlank(message = "domain is required")
    String domain,

    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Please enter a valid phone number (7 to 15 digits)")
    String contactNumber,
    String preferredLanguage,
    String location
) {}
