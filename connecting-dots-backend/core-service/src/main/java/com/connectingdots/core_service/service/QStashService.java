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

    public void publishToAiService(IngestionMessage message) {
        String destinationEndpoint = qStashApiUrl + aiWebhookUrl;
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