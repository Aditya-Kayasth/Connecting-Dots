package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProblemStatementRepository problemStatementRepository;

    public Application applyToProblem(ApplicationRequest request) {
        // 1. Verify problem statement exists
        ProblemStatement problem = problemStatementRepository.findById(request.problemId())
                .orElseThrow(() -> new RuntimeException("Problem statement not found"));

        // 2. Ensure problem statement status allows applications
        String status = problem.getStatus().name();
        if (!"OPEN".equalsIgnoreCase(status) && !"PROCESSED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Problem statement is not open for applications");
        }

        // 3. Check for duplicate application
        boolean alreadyApplied = applicationRepository.existsByProblemIdAndContributorProfileId(
                request.problemId(), request.contributorProfileId()
        );

        if (alreadyApplied) {
            throw new IllegalStateException("You have already applied to this problem statement");
        }

        // 4. Save and return the new application
        Application application = Application.builder()
                .problemId(request.problemId())
                .contributorProfileId(request.contributorProfileId())
                .status("PENDING")
                .build();

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsByContributor(UUID contributorProfileId) {
        return applicationRepository.findByContributorProfileId(contributorProfileId);
    }

    public List<Application> getApplicationsForProblem(UUID problemId) {
        return applicationRepository.findByProblemId(problemId);
    }
}