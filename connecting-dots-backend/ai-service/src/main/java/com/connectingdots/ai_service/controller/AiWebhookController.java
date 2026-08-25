package com.connectingdots.ai_service.controller;

import com.connectingdots.ai_service.dto.IngestionMessage;
import com.connectingdots.ai_service.service.AiProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiWebhookController {

    private final AiProcessingService aiProcessingService;

    public AiWebhookController(AiProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleQStashWebhook(
            @RequestBody IngestionMessage message,
            @RequestHeader(value = "Upstash-Signature", required = false) String signature
    ) {
        System.out.println("=================================================");
        System.out.println("[WEBHOOK RECEIVED] Incoming QStash payload!");
        System.out.println("Problem ID: " + message.problemId());
        System.out.println("File URL: " + message.sourceFileUrl());
        System.out.println("=================================================");

        // Hand off to background thread
        aiProcessingService.processFileAndExtractProblem(message);

        return ResponseEntity.ok("Webhook accepted and processing asynchronously.");
    }
}