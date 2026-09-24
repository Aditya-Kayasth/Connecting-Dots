package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest;
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
import java.util.List;
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
    @Mock private DemoAccountGuard demoAccountGuard;

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
        when(applicationRepository.findByProblemIdAndContributorProfileId(problemId, contributorProfileId)).thenReturn(Optional.empty());
        
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

        Application existingApp = new Application();
        existingApp.setStatus("PENDING");

        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.findByProblemIdAndContributorProfileId(problemId, contributorProfileId)).thenReturn(Optional.of(existingApp));

        assertThrows(IllegalStateException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void applyToProblem_ReapplyWithdrawnSuccess() {
        UUID problemId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.OPEN);

        Application withdrawnApp = new Application();
        withdrawnApp.setId(UUID.randomUUID());
        withdrawnApp.setProblemId(problemId);
        withdrawnApp.setContributorProfileId(contributorProfileId);
        withdrawnApp.setStatus("WITHDRAWN");

        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.findByProblemIdAndContributorProfileId(problemId, contributorProfileId)).thenReturn(Optional.of(withdrawnApp));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        Application result = applicationService.applyToProblem(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(applicationRepository, times(1)).save(withdrawnApp);
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

    @Test
    void updateApplicationStatus_AcceptApplication_AutoRejectsOthersAndSetsInProgress() {
        UUID problemId = UUID.randomUUID();
        UUID app1Id = UUID.randomUUID();
        UUID app2Id = UUID.randomUUID();

        ProblemStatement problem = new ProblemStatement();
        problem.setId(problemId);
        problem.setStatus(ProblemStatement.Status.OPEN);

        User ngoUser = User.builder().email("ngo@test.com").role(User.Role.NGO).build();
        ngoUser.setId(callerUserId);

        NgoProfile ngoProfile = NgoProfile.builder().user(ngoUser).build();
        problem.setNgoProfile(ngoProfile);

        Application app1 = new Application();
        app1.setId(app1Id);
        app1.setProblemId(problemId);
        app1.setStatus("PENDING");

        Application app2 = new Application();
        app2.setId(app2Id);
        app2.setProblemId(problemId);
        app2.setStatus("PENDING");

        when(applicationRepository.findById(app1Id)).thenReturn(Optional.of(app1));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(ngoUser));
        when(userRepository.findById(callerUserId)).thenReturn(Optional.of(ngoUser));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(applicationRepository.findByProblemId(problemId)).thenReturn(List.of(app1, app2));

        ApplicationStatusUpdateRequest updateReq = new ApplicationStatusUpdateRequest("ACCEPTED");
        Application result = applicationService.updateApplicationStatus(app1Id, updateReq, null);

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(problem.getStatus()).isEqualTo(ProblemStatement.Status.IN_PROGRESS);
        assertThat(app2.getStatus()).isEqualTo("REJECTED");
        verify(problemStatementRepository, times(1)).save(problem);
    }

    @Test
    void updateApplicationStatus_WithdrawAcceptedApplication_RevertsPsToOpen() {
        UUID problemId = UUID.randomUUID();
        UUID app1Id = UUID.randomUUID();

        ProblemStatement problem = new ProblemStatement();
        problem.setId(problemId);
        problem.setStatus(ProblemStatement.Status.IN_PROGRESS);

        User ngoUser = User.builder().email("ngo@test.com").role(User.Role.NGO).build();
        ngoUser.setId(callerUserId);
        NgoProfile ngoProfile = NgoProfile.builder().user(ngoUser).build();
        problem.setNgoProfile(ngoProfile);

        Application app1 = new Application();
        app1.setId(app1Id);
        app1.setProblemId(problemId);
        app1.setStatus("ACCEPTED");

        when(applicationRepository.findById(app1Id)).thenReturn(Optional.of(app1));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(ngoUser));
        when(userRepository.findById(callerUserId)).thenReturn(Optional.of(ngoUser));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(applicationRepository.findByProblemId(problemId)).thenReturn(List.of(app1));

        ApplicationStatusUpdateRequest updateReq = new ApplicationStatusUpdateRequest("WITHDRAWN");
        Application result = applicationService.updateApplicationStatus(app1Id, updateReq, null);

        assertThat(result.getStatus()).isEqualTo("WITHDRAWN");
        assertThat(problem.getStatus()).isEqualTo(ProblemStatement.Status.OPEN);
        verify(problemStatementRepository, times(1)).save(problem);
    }
}
