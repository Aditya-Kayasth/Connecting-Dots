package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemStatementControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private NgoProfileRepository ngoProfileRepository;
    @Autowired private ContributorProfileRepository contributorProfileRepository;
    @Autowired private ProblemStatementRepository problemStatementRepository;
    @Autowired private JwtUtil jwtUtil;

    private String ngoWithProfileToken;
    private String ngoWithoutProfileToken;
    private String contributorToken;

    @BeforeEach
    void setUp() {
        problemStatementRepository.deleteAll();
        ngoProfileRepository.deleteAll();
        contributorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // 1. NGO User WITH Profile
        User ngoUser1 = User.builder().email("ngo1@org.com").passwordHash("hash").role(User.Role.NGO).isActive(true).build();
        userRepository.save(ngoUser1);
        NgoProfile profile = NgoProfile.builder().user(ngoUser1).organizationName("Org 1").domain("Tech").build();
        ngoProfileRepository.save(profile);
        ngoWithProfileToken = jwtUtil.generateToken(ngoUser1.getEmail(), ngoUser1.getRole().name());

        // 2. NGO User WITHOUT Profile
        User ngoUser2 = User.builder().email("ngo2@org.com").passwordHash("hash").role(User.Role.NGO).isActive(true).build();
        userRepository.save(ngoUser2);
        ngoWithoutProfileToken = jwtUtil.generateToken(ngoUser2.getEmail(), ngoUser2.getRole().name());

        // 3. Contributor User
        User contributor = User.builder().email("contributor@dev.com").passwordHash("hash").role(User.Role.CONTRIBUTOR).isActive(true).build();
        userRepository.save(contributor);
        contributorToken = jwtUtil.generateToken(contributor.getEmail(), contributor.getRole().name());
    }

    @Test
    void createProblemStatement_Success() throws Exception {
        ProblemStatementRequest request = new ProblemStatementRequest("App", "Needs an app", "Tech", null, null, null);
        mockMvc.perform(post("/api/v1/core/problem-statements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoWithProfileToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createProblemStatement_NgoWithoutProfile_Fails() throws Exception {
        ProblemStatementRequest request = new ProblemStatementRequest("App", "Needs an app", "Tech", null, null, null);
        mockMvc.perform(post("/api/v1/core/problem-statements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoWithoutProfileToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProblemStatement_Contributor_Fails() throws Exception {
        ProblemStatementRequest request = new ProblemStatementRequest("App", "Needs an app", "Tech", null, null, null);
        mockMvc.perform(post("/api/v1/core/problem-statements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
