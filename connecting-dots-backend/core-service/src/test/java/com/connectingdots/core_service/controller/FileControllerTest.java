package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.CloudinarySignatureResponse;
import com.connectingdots.core_service.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @Autowired
    private com.connectingdots.core_service.security.JwtUtil jwtUtil;

    @Test
    void shouldReturnUploadSignatureWhenAuthenticated() throws Exception {
        String token = jwtUtil.generateToken("ngo@example.com", "NGO");
        
        CloudinarySignatureResponse mockResponse = new CloudinarySignatureResponse(
                "mock-signature",
                1234567890L,
                "mock-api-key",
                "mock-cloud-name",
                "connecting-dots/problems"
        );

        when(fileService.generateUploadSignature(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/core/files/signature")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "connecting-dots/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signature").value("mock-signature"))
                .andExpect(jsonPath("$.timestamp").value(1234567890L));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/core/files/signature"))
                .andExpect(status().isUnauthorized());
    }
}
