package com.connectingdots.ai_service.service;

import com.connectingdots.ai_service.dto.AiExtractionResult;
import com.connectingdots.ai_service.dto.IngestionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
            // 2. Fetch the existing problem statement to get the NGO's free-form text instructions (if any)
            String problemUrl = coreServiceUrl + "/api/v1/core/problem-statements/" + message.problemId();
            com.connectingdots.ai_service.dto.ProblemStatementRequest existing = null;
            try {
                existing = restClient.get()
                        .uri(problemUrl)
                        .retrieve()
                        .body(com.connectingdots.ai_service.dto.ProblemStatementRequest.class);
            } catch (Exception ex) {
                System.err.println("Failed to fetch existing problem statement: " + ex.getMessage());
            }

            // Extract the user-supplied text description (which contains their typed instructions)
            String typedInstructions = (existing != null && existing.description() != null) ? existing.description().trim() : "";

            // 3. Download raw file bytes if an uploaded image/document is attached
            byte[] fileBytes = null;
            String mimeType = null;
            if (message.sourceFileUrl() != null && !message.sourceFileUrl().isBlank()) {
                try {
                    fileBytes = restClient.get()
                            .uri(message.sourceFileUrl())
                            .retrieve()
                            .body(byte[].class);
                    mimeType = resolveMimeType(message.sourceType(), message.sourceFileUrl());
                    System.out.println("Downloaded source file bytes successfully (" + (fileBytes != null ? fileBytes.length : 0) + " bytes, MIME: " + mimeType + ")");
                } catch (Exception dlEx) {
                    System.err.println("Note: Could not fetch raw file bytes for direct multimodal stream, using URL reference fallback: " + dlEx.getMessage());
                }
            }

            // 4. Construct a unified prompt handling Text Only, Image/File Only, or Both
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are an expert technical writer and software architect. A non-technical NGO user has submitted a request for a software solution.\n\n");
            
            if (message.sourceFileUrl() != null && !message.sourceFileUrl().isBlank()) {
                promptBuilder.append("An uploaded source document/image is provided with this submission (URL: ")
                        .append(message.sourceFileUrl()).append(").\n");
            }
            if (!typedInstructions.isEmpty()) {
                promptBuilder.append("The NGO user provided these specific text instructions / problem context:\n\"")
                        .append(typedInstructions).append("\"\n");
            }
            
            promptBuilder.append("\nAnalyze the provided input (image/document if attached, and text instructions if provided) to synthesize a professional, clear, and structured technical problem brief for software developers.\n")
                    .append("Your response MUST be a JSON object with the following fields:\n")
                    .append("1. title: A concise, action-oriented, professional title (maximum 80 characters).\n")
                    .append("2. description: A clear, objective technical description explaining the problem, target audience, and desired outcome for a developer (150-300 words).\n")
                    .append("3. domain: Categorize it strictly into one of: 'Education Technology', 'Healthcare & Wellness', 'Environment & Sustainability', 'Community Development', 'Poverty Alleviation', 'Financial Inclusion', 'Web/Software Development', or 'Others'.\n\n")
                    .append("Do not include markdown code block formatting or backticks. Return ONLY the raw JSON.");

            String prompt = promptBuilder.toString();

            // 5. Call Gemini 3.5 Flash model with multimodal Parts (Text + Image/File)
            com.google.genai.types.Part textPart = com.google.genai.types.Part.fromText(prompt);
            com.google.genai.types.Part filePart = (fileBytes != null && mimeType != null)
                    ? com.google.genai.types.Part.fromBytes(fileBytes, mimeType)
                    : null;

            GenerateContentResponse response = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    if (filePart != null) {
                        return client.models.generateContent("gemini-3.5-flash", com.google.genai.types.Content.fromParts(textPart, filePart), null);
                    } else {
                        return client.models.generateContent("gemini-3.5-flash", prompt, null);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(120, java.util.concurrent.TimeUnit.SECONDS);

            String aiOutputText = response.text();
            System.out.println("Gemini raw response received successfully.");

            // 4. Map the AI response into your extraction DTO by parsing the JSON output
            AiExtractionResult extractedData = null;
            if (aiOutputText != null && !aiOutputText.isBlank()) {
                try {
                    String cleanJson = extractJsonBlock(aiOutputText);
                    extractedData = objectMapper.readValue(cleanJson, AiExtractionResult.class);
                } catch (Exception jsonEx) {
                    System.err.println("Jackson JSON parsing failed, falling back to raw dump: " + jsonEx.getMessage());
                }
            }

            if (extractedData == null || extractedData.getTitle() == null || extractedData.getTitle().isBlank()) {
                extractedData = AiExtractionResult.builder()
                        .title("Gemini Parsed Problem Statement")
                        .description(aiOutputText != null ? aiOutputText : "No description generated.")
                        .domain("Technology & Impact")
                        .status("PROCESSED")
                        .build();
            } else {
                extractedData.setStatus("PROCESSED");
            }

            // 5. Send results back to core-service via callback
            sendResultsBackToCore(message.problemId().toString(), extractedData);

        } catch (Exception e) {
            System.err.println("Failed to process real AI ingestion: " + e.getMessage());
            try {
                // Fetch the existing problem statement to retrieve its original details
                String problemUrl = coreServiceUrl + "/api/v1/core/problem-statements/" + message.problemId();
                com.connectingdots.ai_service.dto.ProblemStatementRequest existing = restClient.get()
                        .uri(problemUrl)
                        .retrieve()
                        .body(com.connectingdots.ai_service.dto.ProblemStatementRequest.class);

                String title = (existing != null && existing.title() != null && !existing.title().isBlank()) 
                         ? existing.title().trim() 
                         : "Draft Problem Statement (AI Ingestion Failed)";
                String desc = (existing != null && existing.description() != null && !existing.description().isBlank()) 
                         ? existing.description().trim() 
                         : "AI extraction failed due to timeout or configuration error. Please review and enter details manually.";
                String domain = (existing != null && existing.domain() != null && !existing.domain().isBlank()) 
                         ? existing.domain() 
                         : "Others";

                String fallbackDesc = desc + "\n\n(Error details: " + e.getMessage() + ")";

                AiExtractionResult fallbackData = AiExtractionResult.builder()
                        .title(title)
                        .description(fallbackDesc)
                        .domain(domain)
                        .status("PROCESSED")
                        .build();

                sendResultsBackToCore(message.problemId().toString(), fallbackData);
            } catch (Exception ex) {
                System.err.println("Failed to perform direct callback fallback: " + ex.getMessage());
            }
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

    private String extractJsonBlock(String text) {
        if (text == null || text.isBlank()) return "";
        String trimmed = text.trim();
        if (trimmed.contains("```")) {
            int firstFence = trimmed.indexOf("```");
            int lastFence = trimmed.lastIndexOf("```");
            if (firstFence != -1 && lastFence > firstFence) {
                String content = trimmed.substring(firstFence, lastFence + 3);
                trimmed = content.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start != -1 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String resolveMimeType(String sourceType, String url) {
        if (sourceType != null && !sourceType.isBlank()) {
            String st = sourceType.toLowerCase().trim();
            if (st.contains("png")) return "image/png";
            if (st.contains("jpg") || st.contains("jpeg")) return "image/jpeg";
            if (st.contains("pdf")) return "application/pdf";
            if (st.contains("mp3")) return "audio/mp3";
            if (st.contains("wav")) return "audio/wav";
        }
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.endsWith(".png")) return "image/png";
            if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) return "image/jpeg";
            if (lowerUrl.endsWith(".pdf")) return "application/pdf";
            if (lowerUrl.endsWith(".mp3")) return "audio/mp3";
            if (lowerUrl.endsWith(".wav")) return "audio/wav";
        }
        return "image/png";
    }
}