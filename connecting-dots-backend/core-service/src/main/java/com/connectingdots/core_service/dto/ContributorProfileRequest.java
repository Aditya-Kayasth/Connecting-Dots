package com.connectingdots.core_service.dto;

public record ContributorProfileRequest(
        String firstName,
        String lastName,
        String skillsSummary,
        String portfolioUrl
) {}