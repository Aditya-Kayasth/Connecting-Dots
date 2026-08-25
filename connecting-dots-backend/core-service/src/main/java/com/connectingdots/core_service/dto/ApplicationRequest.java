package com.connectingdots.core_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplicationRequest(
        @NotNull UUID problemId,
        @NotNull UUID contributorProfileId) {
}