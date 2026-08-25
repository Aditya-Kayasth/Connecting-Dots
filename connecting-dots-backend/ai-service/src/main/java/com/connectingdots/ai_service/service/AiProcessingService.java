package com.connectingdots.ai_service.service;

import com.connectingdots.ai_service.dto.AiExtractionResult;
import com.connectingdots.ai_service.dto.IngestionMessage;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AiProcessingService {

    private final RestClient restClient = RestClient.create();

    @Value("${core.service.url:http://localhost:8081}")
    private String coreServiceUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Async
    public void processFileAndExtractProblem(IngestionMessage message) {
        try {
            System.out.println("=================================================");
            System.out.println("Starting real Gemini AI processing for ID: " + message.problemId());
            System.out.println("Target File URL: " + message.sourceFileUrl());
            System.out.println("=================================================");

            // 1. Initialize the official Google GenAI Client
            Client client = Client.builder().apiKey(geminiApiKey).build();

            // 2. Construct the prompt directing Gemini to analyze the document URL
            String prompt = "Analyze the document at this URL: " + message.sourceFileUrl() + 
                    ". Extract a clean title, a detailed description, and categorize it into a domain " +
                    "(e.g., Sustainability, Healthcare, Education, FinTech). Return the result clearly.";

            // 3. Call the Gemini 3.5 Flash model
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.5-flash", 
                    prompt, 
                    null
            );

            String aiOutputText = response.text();
            System.out.println("Gemini raw response received successfully.");

            // 4. Map the AI response into your extraction DTO
            AiExtractionResult extractedData = AiExtractionResult.builder()
                    .title("Gemini Parsed Problem Statement")
                    .description(aiOutputText != null ? aiOutputText : "No description generated.")
                    .domain("Technology & Impact")
                    .status("PROCESSED")
                    .build();

            // 5. Send results back to core-service via callback
            sendResultsBackToCore(message.problemId().toString(), extractedData);

        } catch (Exception e) {
            System.err.println("Failed to process real AI ingestion: " + e.getMessage());
        }
    }

    private void sendResultsBackToCore(String problemId, AiExtractionResult data) {
        String callbackUrl = coreServiceUrl + "/api/v1/core/problem-statements/" + problemId + "/ai-update";
        
        try {
            String response = restClient.put()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(data)
                    .retrieve()
                    .body(String.class);

            System.out.println("Successfully synced real AI results back to core-service!");
            System.out.println("Core Response: " + response);
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to call core-service callback: " + e.getMessage());
        }
    }
}