package com.connectingdots.ai_service.controller;

import com.connectingdots.ai_service.service.AiProblemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiProblemService aiProblemService;

    @Test
    void structureProblem_Success() throws Exception {
        String rawText = "We are a small charity that needs an application to manage our volunteers.";
        String token = "dummy-jwt-token";
        
        // Mock the service call to prevent hitting the real Gemini API or Core Service
        when(aiProblemService.structureProblem(eq(rawText), any())).thenReturn(new Object());

        mockMvc.perform(post("/api/v1/ai/structure-problem")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.TEXT_PLAIN)
                .content(rawText))
                .andExpect(status().isOk());
    }

    @Test
    void translateProblemContent_Success() throws Exception {
        com.connectingdots.ai_service.dto.TranslationRequest request =
                new com.connectingdots.ai_service.dto.TranslationRequest("Title", "Desc", "hi");

        com.connectingdots.ai_service.dto.TranslationResponse mockResponse =
                new com.connectingdots.ai_service.dto.TranslationResponse("शीर्षक", "विवरण", "hi");

        when(aiProblemService.translateProblemContent(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/ai/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
