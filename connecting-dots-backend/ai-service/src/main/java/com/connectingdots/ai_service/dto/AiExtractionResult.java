package com.connectingdots.ai_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiExtractionResult {
    private String title;
    private String description;
    private String domain;
    private String status;
}