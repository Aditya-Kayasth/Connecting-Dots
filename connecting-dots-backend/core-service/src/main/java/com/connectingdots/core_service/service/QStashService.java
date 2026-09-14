package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.IngestionMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class QStashService {

    private final RestClient restClient = RestClient.create();

    @Value("${qstash.token}")
    private String qStashToken;

    @Value("${qstash.api-url}")
    private String qStashApiUrl;

    @Value("${qstash.ai-webhook-url}")
    private String aiWebhookUrl;

    @Value("${ai.service.url:http://localhost:8082}")
    private String aiServiceUrl;

    public void publishToAiService(IngestionMessage message) {
        boolean isLocalWebhook = aiWebhookUrl != null && (aiWebhookUrl.contains("localhost") || aiWebhookUrl.contains("127.0.0.1"));

        if (qStashToken == null || qStashToken.isBlank() || isLocalWebhook) {
            System.out.println("Local environment detected. Dispatching direct webhook to AI service...");
            String localWebhookUrl = aiServiceUrl + "/api/v1/ai/webhook";
            try {
                restClient.post()
                        .uri(java.net.URI.create(localWebhookUrl))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(message)
                        .retrieve()
                        .toBodilessEntity();
                System.out.println("Successfully dispatched direct Local AI Webhook callback!");
                return;
            } catch (Exception e) {
                System.err.println("Failed to perform direct Local AI Webhook callback: " + e.getMessage());
            }
            if (qStashToken == null || qStashToken.isBlank()) return;
        }

        String destinationEndpoint = qStashApiUrl.endsWith("/") || aiWebhookUrl.startsWith("/") 
                ? qStashApiUrl + aiWebhookUrl 
                : qStashApiUrl + "/" + aiWebhookUrl;
        try {
            String response = restClient.post()
                    .uri(java.net.URI.create(destinationEndpoint))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + qStashToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .body(String.class);

            System.out.println("=================================================");
            System.out.println("QStash Publish SUCCESS: " + response);
            System.out.println("=================================================");
        } catch (RestClientResponseException e) {
            System.err.println("=================================================");
            System.err.println("QSTASH API REJECTED REQUEST");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            System.err.println("Attempted URL: " + destinationEndpoint);
            System.err.println("=================================================");
        } catch (Exception e) {
            System.err.println("Failed to reach QStash entirely: " + e.getMessage());
        }
    }
}