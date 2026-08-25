package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ContributorProfileRequest;
import com.connectingdots.core_service.dto.NgoProfileRequest;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.security.JwtUtil;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTest {

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
    private JwtUtil jwtUtil;

    @Autowired
    private ProblemStatementRepository problemStatementRepository;

    private String ngoToken;
    private String contributorToken;

    @BeforeEach
    void setUp() {
        problemStatementRepository.deleteAll();
        ngoProfileRepository.deleteAll();
        contributorProfileRepository.deleteAll();
        userRepository.deleteAll();

        User ngoUser = User.builder()
                .email("admin@ngo.org")
                .passwordHash("hashedpassword")
                .role(User.Role.NGO)
                .isActive(true)
                .build();
        userRepository.save(ngoUser);
        ngoToken = jwtUtil.generateToken(ngoUser.getEmail(), ngoUser.getRole().name());

        User contributorUser = User.builder()
                .email("contributor@example.com")
                .passwordHash("hashedpassword")
                .role(User.Role.CONTRIBUTOR)
                .isActive(true)
                .build();
        userRepository.save(contributorUser);
        contributorToken = jwtUtil.generateToken(contributorUser.getEmail(), contributorUser.getRole().name());
    }

    @Test
    void createNgoProfile_Success() throws Exception {
        NgoProfileRequest request = new NgoProfileRequest("Test NGO", "Education", "1234567890");

        mockMvc.perform(post("/api/v1/core/profiles/ngo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createContributorProfile_Success() throws Exception {
        ContributorProfileRequest request = new ContributorProfileRequest("John", "Doe", "Java, Spring", "http://portfolio.com");

        mockMvc.perform(post("/api/v1/core/profiles/contributor")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createNgoProfile_WithContributorRole_ShouldFail() throws Exception {
        NgoProfileRequest request = new NgoProfileRequest("Test NGO", "Education", "1234567890");

        mockMvc.perform(post("/api/v1/core/profiles/ngo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
