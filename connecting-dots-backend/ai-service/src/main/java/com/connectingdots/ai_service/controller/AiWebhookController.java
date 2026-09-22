package com.connectingdots.ai_service.controller;

import com.connectingdots.ai_service.dto.IngestionMessage;
import com.connectingdots.ai_service.service.AiProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiWebhookController {

    private final AiProcessingService aiProcessingService;

    @Value("${internal.service.secret:}")
    private String internalServiceSecret;

    public AiWebhookController(AiProcessingService aiProcessingService) {
        this.aiProcessingService = aiProcessingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleQStashWebhook(
            @RequestBody IngestionMessage message,
            @RequestHeader(value = "Upstash-Signature", required = false) String qstashSignature,
            @RequestHeader(value = "X-Internal-Service-Secret", required = false) String serviceSecret
    ) {
        // Validate internal service secret — reject unauthenticated callers
        boolean validSecret = internalServiceSecret != null
                && !internalServiceSecret.isBlank()
                && internalServiceSecret.equals(serviceSecret);
        // Also allow genuine QStash deliveries (they carry Upstash-Signature)
        boolean isQStash = qstashSignature != null && !qstashSignature.isBlank();

        if (!validSecret && !isQStash) {
            System.err.println("[WEBHOOK REJECTED] Missing or invalid service secret.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized webhook call.");
        }

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