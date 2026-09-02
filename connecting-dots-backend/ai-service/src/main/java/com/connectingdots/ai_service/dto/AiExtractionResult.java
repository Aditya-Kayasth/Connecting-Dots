package com.connectingdots.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExtractionResult {
    private String title;
    private String description;
    private String domain;
    private String status;
}