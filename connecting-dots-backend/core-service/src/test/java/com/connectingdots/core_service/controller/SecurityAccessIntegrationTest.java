package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private ContributorProfileRepository contributorProfileRepository;

    @Autowired
    private ProblemStatementRepository problemStatementRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String ngoToken;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        problemStatementRepository.deleteAll();
        ngoProfileRepository.deleteAll();
        contributorProfileRepository.deleteAll();
        userRepository.deleteAll();

        User ngoUser = User.builder()
                .email("ngo.security@example.com")
                .passwordHash("hashedpassword")
                .role(User.Role.NGO)
                .isActive(true)
                .build();
        userRepository.save(ngoUser);

        com.connectingdots.core_service.entity.NgoProfile ngoProfile = com.connectingdots.core_service.entity.NgoProfile.builder()
                .user(ngoUser)
                .organizationName("Security NGO")
                .domain("Technology")
                .contactNumber("1234567890")
                .preferredLanguage("en")
                .build();
        ngoProfileRepository.save(ngoProfile);

        ngoToken = jwtUtil.generateToken(ngoUser.getEmail(), ngoUser.getRole().name());
    }

    @Test
    void unauthenticatedGuest_CanQueryPublicDiscoveryEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/core/problem-statements"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/profiles/ngos"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/profiles/contributors"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedGuest_AttemptingMutation_Returns401Unauthorized() throws Exception {
        ApplicationRequest applicationRequest = new ApplicationRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/core/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applicationRequest)))
                .andExpect(status().isUnauthorized());

        ProblemStatementRequest problemRequest = new ProblemStatementRequest(
                "Test Problem", "Description", "Healthcare", null, null, null
        );

        mockMvc.perform(post("/api/v1/core/problem-statements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(problemRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUser_WithValidJwtToken_CanExecutePermittedWrite() throws Exception {
        ProblemStatementRequest problemRequest = new ProblemStatementRequest(
                "Authorized Problem", "Detailed Description", "Technology", null, null, null
        );

        mockMvc.perform(post("/api/v1/core/problem-statements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(problemRequest)))
                .andExpect(status().isCreated());
    }
}
