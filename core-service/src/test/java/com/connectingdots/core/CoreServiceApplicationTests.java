package com.connectingdots.core;

import com.connectingdots.core.controller.ProblemController.ProblemSubmitRequest;
import com.connectingdots.core.domain.NgoProblemStatement;
import com.connectingdots.core.repository.NgoProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CoreServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NgoProblemRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testProblemSubmissionAndStateTransition() throws Exception {
        // 1. Submit a new problem
        ProblemSubmitRequest request = new ProblemSubmitRequest("Test NGO", "We need help organizing data.");
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/problems/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ngoName").value("Test NGO"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.structuredProblem").value("Structured: We need help organizing data."))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        NgoProblemStatement problem = objectMapper.readValue(responseBody, NgoProblemStatement.class);
        UUID problemId = problem.getId();
        
        assertThat(problemId).isNotNull();

        // 2. Query open problems
        mockMvc.perform(get("/api/v1/problems/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(problemId.toString()));

        // 3. Update status to WORK_IN_PROGRESS
        mockMvc.perform(patch("/api/v1/problems/" + problemId + "/status")
                .param("status", "WORK_IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WORK_IN_PROGRESS"));

        // Verify status is updated in DB
        NgoProblemStatement updatedProblem = repository.findById(problemId).orElseThrow();
        assertThat(updatedProblem.getStatus()).isEqualTo("WORK_IN_PROGRESS");
    }

    @Test
    public void testCircuitBreakerFallback() throws Exception {
        // Submit a problem that triggers the error fallback simulated in ProblemService
        ProblemSubmitRequest request = new ProblemSubmitRequest("Error NGO", "trigger_error");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/problems/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.structuredProblem").value("Fallback structure: Could not reach AI service. Raw description saved."));
    }
}
