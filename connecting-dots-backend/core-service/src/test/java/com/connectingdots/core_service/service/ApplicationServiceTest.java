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
}
