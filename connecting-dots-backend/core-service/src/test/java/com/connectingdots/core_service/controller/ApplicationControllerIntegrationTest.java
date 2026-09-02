package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private NgoProfileRepository ngoProfileRepository;
    @Autowired private ContributorProfileRepository contributorProfileRepository;
    @Autowired private ProblemStatementRepository problemStatementRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private JwtUtil jwtUtil;

    private String contributorToken;
    private String ngoToken;
    private String otherNgoToken;
    private ContributorProfile contributorProfile;
    private ProblemStatement openProblemStatement;
    private ProblemStatement closedProblemStatement;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        problemStatementRepository.deleteAll();
        ngoProfileRepository.deleteAll();
        contributorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // NGO Setup
        User ngoUser = User.builder().email("ngo@org.com").passwordHash("hash").role(User.Role.NGO).isActive(true).build();
        userRepository.save(ngoUser);
        NgoProfile ngoProfile = NgoProfile.builder().user(ngoUser).organizationName("Org 1").domain("Tech").build();
        ngoProfileRepository.save(ngoProfile);
        ngoToken = jwtUtil.generateToken(ngoUser.getEmail(), ngoUser.getRole().name());

        User otherNgoUser = User.builder().email("other@org.com").passwordHash("hash").role(User.Role.NGO).isActive(true).build();
        userRepository.save(otherNgoUser);
        NgoProfile otherNgoProfile = NgoProfile.builder().user(otherNgoUser).organizationName("Org 2").domain("Tech").build();
        ngoProfileRepository.save(otherNgoProfile);
        otherNgoToken = jwtUtil.generateToken(otherNgoUser.getEmail(), otherNgoUser.getRole().name());

        // Contributor Setup
        User contributorUser = User.builder().email("contributor@dev.com").passwordHash("hash").role(User.Role.CONTRIBUTOR).isActive(true).build();
        userRepository.save(contributorUser);
        contributorProfile = ContributorProfile.builder().user(contributorUser).firstName("John").lastName("Doe").build();
        contributorProfileRepository.save(contributorProfile);
        contributorToken = jwtUtil.generateToken(contributorUser.getEmail(), contributorUser.getRole().name());

        // Problem Statements
        openProblemStatement = ProblemStatement.builder()
                .ngoProfile(ngoProfile)
                .title("Open App")
                .description("Needs an open app")
                .domain("Tech")
                .status(ProblemStatement.Status.OPEN)
                .build();
        problemStatementRepository.save(openProblemStatement);

        closedProblemStatement = ProblemStatement.builder()
                .ngoProfile(ngoProfile)
                .title("Closed App")
                .description("Needs a closed app")
                .domain("Tech")
                .status(ProblemStatement.Status.CLOSED)
                .build();
        problemStatementRepository.save(closedProblemStatement);
    }

    @Test
    void applyToProblem_Success() throws Exception {
        ApplicationRequest request = new ApplicationRequest(openProblemStatement.getId(), contributorProfile.getId());

        mockMvc.perform(post("/api/v1/core/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<Application> applications = applicationRepository.findAll();
        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(applications.get(0).getProblemId()).isEqualTo(openProblemStatement.getId());
        assertThat(applications.get(0).getContributorProfileId()).isEqualTo(contributorProfile.getId());
    }

    @Test
    void applyToProblem_DuplicateFails() throws Exception {
        Application application = Application.builder()
                .problemId(openProblemStatement.getId())
                .contributorProfileId(contributorProfile.getId())
                .status("PENDING")
                .build();
        applicationRepository.save(application);

        ApplicationRequest request = new ApplicationRequest(openProblemStatement.getId(), contributorProfile.getId());

        mockMvc.perform(post("/api/v1/core/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyToProblem_ClosedStatusFails() throws Exception {
        ApplicationRequest request = new ApplicationRequest(closedProblemStatement.getId(), contributorProfile.getId());

        mockMvc.perform(post("/api/v1/core/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + contributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateApplicationStatus_Success() throws Exception {
        Application application = Application.builder()
                .problemId(openProblemStatement.getId())
                .contributorProfileId(contributorProfile.getId())
                .status("PENDING")
                .build();
        application = applicationRepository.save(application);

        com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest statusUpdateRequest =
                new com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest("ACCEPTED");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/core/applications/" + application.getId() + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ngoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk());

        Application updated = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void updateApplicationStatus_OtherNgo_Fails() throws Exception {
        Application application = Application.builder()
                .problemId(openProblemStatement.getId())
                .contributorProfileId(contributorProfile.getId())
                .status("PENDING")
                .build();
        Application savedApp = applicationRepository.save(application);

        com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest statusUpdateRequest =
                new com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest("ACCEPTED");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/core/applications/" + savedApp.getId() + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherNgoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isForbidden());
    }
}
