package com.connectingdots.core_service.dto;

public record NgoProfileRequest(
        String organizationName,
        String domain,
        String contactNumber
) {}