package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProblemStatementRepository problemStatementRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final UserRepository userRepository;

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

    public Application updateApplicationStatus(UUID applicationId, ApplicationStatusUpdateRequest request, UUID requestingNgoProfileId) {
        // 1. Fetch Application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // 2. Fetch Problem Statement
        ProblemStatement problem = problemStatementRepository.findById(application.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem statement not found"));

        // Resolve NGO profile ID if not provided
        UUID ngoProfileId = requestingNgoProfileId;
        if (ngoProfileId == null) {
            ngoProfileId = getAuthenticatedNgoProfileId();
        }

        // 3. Strict Ownership Security Check
        if (problem.getNgoProfile() == null || !problem.getNgoProfile().getId().equals(ngoProfileId)) {
            throw new SecurityException("You are not authorized to update applications for this problem statement");
        }

        // 4. Update status and save
        application.setStatus(request.status());
        return applicationRepository.save(application);
    }

    public Application updateApplicationStatus(UUID applicationId, ApplicationStatusUpdateRequest request) {
        return updateApplicationStatus(applicationId, request, null);
    }

    public Application completeApplication(UUID applicationId, UUID requestingNgoProfileId) {
        // 1. Fetch Application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // 2. Fetch Problem Statement
        ProblemStatement problem = problemStatementRepository.findById(application.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem statement not found"));

        // Resolve NGO profile ID if not provided
        UUID ngoProfileId = requestingNgoProfileId;
        if (ngoProfileId == null) {
            ngoProfileId = getAuthenticatedNgoProfileId();
        }

        // 3. Strict Ownership Security Check
        if (problem.getNgoProfile() == null || !problem.getNgoProfile().getId().equals(ngoProfileId)) {
            throw new SecurityException("You are not authorized to complete applications for this problem statement");
        }

        // 4. Mark application as COMPLETED
        application.setStatus("COMPLETED");
        Application savedApplication = applicationRepository.save(application);

        // 5. Event-Driven Stat Recomputation: Increment contributor's completed projects count
        contributorProfileRepository.findById(application.getContributorProfileId()).ifPresent(profile -> {
            int currentCompleted = profile.getCompletedProjects() != null ? profile.getCompletedProjects() : 0;
            profile.setCompletedProjects(currentCompleted + 1);
            contributorProfileRepository.save(profile);
        });

        return savedApplication;
    }

    public Application completeApplication(UUID applicationId) {
        return completeApplication(applicationId, null);
    }

    private UUID getAuthenticatedNgoProfileId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new SecurityException("User is not authenticated");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (user.getRole() != User.Role.NGO) {
            throw new SecurityException("Only NGOs can update application status");
        }

        NgoProfile ngoProfile = ngoProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("NGO profile not found"));

        return ngoProfile.getId();
    }
}