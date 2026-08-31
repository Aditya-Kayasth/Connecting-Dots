package com.connectingdots.core_service.dto;

import java.util.UUID;

public record ApplicationDetailResponse(
    UUID id,
    String status,
    String problemTitle,
    String problemDescription,
    String ngoName,
    String applicantName,
    String applicantEmail
) {}
