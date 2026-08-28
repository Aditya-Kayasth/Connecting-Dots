package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String ngoToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin_test@connectingdots.org", "ROLE_ADMIN");
        ngoToken = jwtUtil.generateToken("ngo_test@connectingdots.org", "ROLE_NGO");
    }

    @Test
    void getPlatformStats_WithAdminToken_ReturnsStats() throws Exception {
        mockMvc.perform(get("/api/v1/core/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.totalNgos").exists());
    }

    @Test
    void getPlatformStats_WithoutAdminToken_ReturnsForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/core/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllNgos_WithAdminToken_ReturnsNgoList() throws Exception {
        mockMvc.perform(get("/api/v1/core/admin/ngos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
