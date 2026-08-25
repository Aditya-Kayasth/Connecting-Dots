package com.connectingdots.ai_service.dto;

import java.util.UUID;

public record IngestionMessage(
        UUID problemId,
        String sourceFileUrl,
        String sourceType
) {}