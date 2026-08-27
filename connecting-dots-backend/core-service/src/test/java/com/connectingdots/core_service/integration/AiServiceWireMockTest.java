package com.connectingdots.core_service.integration;

import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.service.ProblemStatementService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceWireMockTest {

    private static WireMockServer wireMockServer;

    @Mock
    private ProblemStatementRepository problemStatementRepository;

    @Mock
    private NgoProfileRepository ngoProfileRepository;

    @Mock
    private ContributorProfileRepository contributorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProblemStatementService problemStatementService;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8082);
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void getProblemStatementById_WithTranslation_WireMockSuccess() {
        UUID problemId = UUID.randomUUID();

        ProblemStatement problem = ProblemStatement.builder()
                .title("Clean Energy Initiative")
                .description("Build solar powered water pumps")
                .status(ProblemStatement.Status.OPEN)
                .build();
        problem.setId(problemId);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problem));

        // Stub WireMock endpoint for ai-service translation
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/translate"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "translatedTitle": "स्वच्छ ऊर्जा उपक्रम",
                                  "translatedDescription": "सौर उर्जेवर चालणारे पाण्याचे पंप तयार करा",
                                  "targetLanguage": "mr"
                                }
                                """)));

        ProblemStatement result = problemStatementService.getProblemStatementById(problemId, "mr");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("स्वच्छ ऊर्जा उपक्रम");
        assertThat(result.getDescription()).isEqualTo("सौर उर्जेवर चालणारे पाण्याचे पंप तयार करा");
    }

    @Test
    void getProblemStatementById_WhenAiServiceFails_FallsBackToOriginal() {
        UUID problemId = UUID.randomUUID();

        ProblemStatement problem = ProblemStatement.builder()
                .title("Original Title")
                .description("Original Description")
                .status(ProblemStatement.Status.OPEN)
                .build();
        problem.setId(problemId);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problem));

        // Stub WireMock endpoint to return HTTP 500 error
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/ai/translate"))
                .willReturn(aResponse().withStatus(500)));

        ProblemStatement result = problemStatementService.getProblemStatementById(problemId, "hi");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Original Title");
        assertThat(result.getDescription()).isEqualTo("Original Description");
    }
}
