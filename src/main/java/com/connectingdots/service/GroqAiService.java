package com.connectingdots.service;

import com.connectingdots.domain.NgoProblemRepository;
import com.connectingdots.domain.NgoProblemStatement;
import com.connectingdots.domain.TechCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqAiService {

    private static final String SYSTEM_PROMPT =
            "You are a Business Analyst assistant. Analyze the following NGO problem description and return ONLY a valid JSON object with exactly two keys: \"structuredProblem\" (a clear technical summary of the problem) and \"techCategory\" (must be exactly one of: SOFTWARE_WEB, DATA_SCIENCE_ML, IOT_HARDWARE, PROCESS_AUTOMATION). Do not include any explanation or markdown.";

    private final RestClient restClient;
    private final NgoProblemRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqAiService(
            @Value("${groq.api.key}") String apiKey,
            NgoProblemRepository repository) {
        this.repository = repository;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public NgoProblemStatement processAndSaveProblem(String ngoName, String rawDescription) {
        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", rawDescription)
                )
        );

        JsonNode response = restClient.post()
                .uri("/openai/v1/chat/completions")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        String content = response.get("choices").get(0).get("message").get("content").asText();

        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response content as JSON", e);
        }

        String structuredProblem = parsed.get("structuredProblem").asText();
        String techCategoryStr = parsed.get("techCategory").asText();

        TechCategory techCategory = TechCategory.valueOf(techCategoryStr);

        NgoProblemStatement entity = new NgoProblemStatement(
                null, ngoName, rawDescription, structuredProblem, techCategory, "OPEN"
        );

        return repository.save(entity);
    }
}
