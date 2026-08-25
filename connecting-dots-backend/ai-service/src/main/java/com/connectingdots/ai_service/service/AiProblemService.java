package com.connectingdots.ai_service.service;

import com.connectingdots.ai_service.dto.ProblemStatementRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AiProblemService {

    private final ChatClient chatClient;
    private final RestClient restClient;

   public AiProblemService(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("loadBalancedBuilder") RestClient.Builder restClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.restClient = restClientBuilder.build();
    }

    public Object structureProblem(String rawText, HttpServletRequest request) {
        ProblemStatementRequest structuredRequest = chatClient.prompt()
                .system("You are a strict data-structuring engine for a non-profit tech platform. " +
                        "Analyze the raw problem description and extract the following fields with absolute consistency: "
                        +
                        "1. title: A concise, professional, action-oriented title (maximum 80 characters). " +
                        "2. description: A clear, objective rewrite of the problem statement suitable for software developers (150-300 words). "
                        +
                        "3. domain: You MUST categorize the problem strictly into one of these exact strings and nothing else: "
                        +
                        "'Education Technology', 'Healthcare & Wellness', 'Environment & Sustainability', 'Community Development', 'Poverty Alleviation', 'Financial Inclusion', 'Web/Software Development', or 'Others'. "
                        +
                        "Return ONLY raw JSON without any markdown formatting or backticks.")
                .user(rawText)
                .call()
                .entity(ProblemStatementRequest.class);

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        return restClient.post()
                .uri("http://core-service/api/v1/core/problem-statements")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(structuredRequest)
                .retrieve()
                .body(Object.class);
    }
}