package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ProblemStatementRepository problemStatementRepository;
    @Mock private ContributorProfileRepository contributorProfileRepository;
    @Mock private NgoProfileRepository ngoProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ApplicationService applicationService;

    private User mockUser;
    private ContributorProfile mockProfile;
    private UUID contributorProfileId;
    private UUID callerUserId;

    @BeforeEach
    void setUp() {
        callerUserId = UUID.randomUUID();
        contributorProfileId = UUID.randomUUID();

        mockUser = User.builder()
                .email("contributor@test.com")
                .role(User.Role.CONTRIBUTOR)
                .build();
        mockUser.setId(callerUserId);

        mockProfile = ContributorProfile.builder()
                .user(mockUser)
                .build();
        mockProfile.setId(contributorProfileId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser.getEmail(), "password", Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyToProblem_Success() {
        UUID problemId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.OPEN);

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.existsByProblemIdAndContributorProfileId(problemId, contributorProfileId)).thenReturn(false);
        
        Application savedApplication = new Application();
        savedApplication.setId(UUID.randomUUID());
        savedApplication.setProblemId(problemId);
        savedApplication.setContributorProfileId(contributorProfileId);
        savedApplication.setStatus("PENDING");

        when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

        Application result = applicationService.applyToProblem(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void applyToProblem_NotFoundFails() {
        UUID problemId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorProfileId);

        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void applyToProblem_DuplicateFails() {
        UUID problemId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.OPEN);

        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.existsByProblemIdAndContributorProfileId(problemId, contributorProfileId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void applyToProblem_StatusClosedFails() {
        UUID problemId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.CLOSED);

        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));

        assertThrows(IllegalStateException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }
}
