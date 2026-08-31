package com.connectingdots.core_service.dto;

import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID problemId,
    UUID contributorProfileId,
    String status,
    String problemTitle,
    String ngoName,
    String applicantName,
    String applicantEmail,
    String applicantSkills
) {}
