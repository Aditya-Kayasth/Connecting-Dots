package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ProblemStatementRepository problemStatementRepository;

    @Mock
    private com.connectingdots.core_service.repository.ContributorProfileRepository contributorProfileRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void applyToProblem_Success() {
        UUID problemId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.OPEN);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.existsByProblemIdAndContributorProfileId(problemId, contributorId)).thenReturn(false);
        
        Application savedApplication = new Application();
        savedApplication.setId(UUID.randomUUID());
        savedApplication.setProblemId(problemId);
        savedApplication.setContributorProfileId(contributorId);
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
        UUID contributorId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorId);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void applyToProblem_DuplicateFails() {
        UUID problemId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.OPEN);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.existsByProblemIdAndContributorProfileId(problemId, contributorId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void applyToProblem_StatusClosedFails() {
        UUID problemId = UUID.randomUUID();
        UUID contributorId = UUID.randomUUID();
        ApplicationRequest request = new ApplicationRequest(problemId, contributorId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setStatus(ProblemStatement.Status.CLOSED);

        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));

        assertThrows(IllegalStateException.class, () -> applicationService.applyToProblem(request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_Success() {
        UUID applicationId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        UUID ngoProfileId = UUID.randomUUID();

        com.connectingdots.core_service.entity.NgoProfile ngoProfile = new com.connectingdots.core_service.entity.NgoProfile();
        ngoProfile.setId(ngoProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setNgoProfile(ngoProfile);

        Application application = new Application();
        application.setId(applicationId);
        application.setProblemId(problemId);
        application.setStatus("PENDING");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest updateRequest =
                new com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest("ACCEPTED");

        Application updated = applicationService.updateApplicationStatus(applicationId, updateRequest, ngoProfileId);

        assertThat(updated.getStatus()).isEqualTo("ACCEPTED");
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    void updateApplicationStatus_UnauthorizedNgo_Fails() {
        UUID applicationId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        UUID ownerNgoId = UUID.randomUUID();
        UUID nonOwnerNgoId = UUID.randomUUID();

        com.connectingdots.core_service.entity.NgoProfile ngoProfile = new com.connectingdots.core_service.entity.NgoProfile();
        ngoProfile.setId(ownerNgoId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setNgoProfile(ngoProfile);

        Application application = new Application();
        application.setId(applicationId);
        application.setProblemId(problemId);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));

        com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest updateRequest =
                new com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest("REJECTED");

        assertThrows(SecurityException.class, () ->
                applicationService.updateApplicationStatus(applicationId, updateRequest, nonOwnerNgoId)
        );
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void completeApplication_Success() {
        UUID applicationId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        UUID ngoProfileId = UUID.randomUUID();
        UUID contributorProfileId = UUID.randomUUID();

        com.connectingdots.core_service.entity.NgoProfile ngoProfile = new com.connectingdots.core_service.entity.NgoProfile();
        ngoProfile.setId(ngoProfileId);

        ProblemStatement problemStatement = new ProblemStatement();
        problemStatement.setId(problemId);
        problemStatement.setNgoProfile(ngoProfile);

        Application application = new Application();
        application.setId(applicationId);
        application.setProblemId(problemId);
        application.setContributorProfileId(contributorProfileId);
        application.setStatus("ACCEPTED");

        com.connectingdots.core_service.entity.ContributorProfile contributorProfile = new com.connectingdots.core_service.entity.ContributorProfile();
        contributorProfile.setId(contributorProfileId);
        contributorProfile.setCompletedProjects(2);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(contributorProfile));

        Application completed = applicationService.completeApplication(applicationId, ngoProfileId);

        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(contributorProfile.getCompletedProjects()).isEqualTo(3);
        verify(contributorProfileRepository, times(1)).save(contributorProfile);
    }
}
