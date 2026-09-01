package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ApplicationDetailResponse;
import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.dto.ApplicationResponse;
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
        // 1. Verify contributor profile exists and caller owns it
        ContributorProfile contributor = contributorProfileRepository.findById(request.contributorProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Contributor profile not found"));

        UUID callerUserId = getAuthenticatedUserId();
        if (contributor.getUser() == null || !contributor.getUser().getId().equals(callerUserId)) {
            throw new SecurityException("You are not authorized to submit applications for this contributor profile");
        }

        // 2. Verify problem statement exists
        ProblemStatement problem = problemStatementRepository.findById(request.problemId())
                .orElseThrow(() -> new IllegalArgumentException("Problem statement not found"));

        // 3. Ensure problem statement status allows applications
        String status = problem.getStatus().name();
        if (!"OPEN".equalsIgnoreCase(status) && !"PROCESSED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Problem statement is not open for applications");
        }

        // 4. Check for duplicate application
        boolean alreadyApplied = applicationRepository.existsByProblemIdAndContributorProfileId(
                request.problemId(), request.contributorProfileId()
        );

        if (alreadyApplied) {
            throw new IllegalStateException("You have already applied to this problem statement");
        }

        // 5. Save and return the new application
        Application application = Application.builder()
                .problemId(request.problemId())
                .contributorProfileId(request.contributorProfileId())
                .status("PENDING")
                .build();

        return applicationRepository.save(application);
    }

    private UUID getAuthenticatedUserId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new SecurityException("User is not authenticated");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public ApplicationDetailResponse getApplicationDetails(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        ProblemStatement problem = problemStatementRepository.findById(application.getProblemId())
                .orElseThrow(() -> new IllegalArgumentException("Problem statement not found"));

        ContributorProfile contributor = contributorProfileRepository.findById(application.getContributorProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Contributor profile not found"));

        String ngoName = (problem.getNgoProfile() != null) ? problem.getNgoProfile().getOrganizationName() : "Unknown NGO";
        String applicantName = contributor.getFirstName() + " " + contributor.getLastName();
        String applicantEmail = (contributor.getUser() != null) ? contributor.getUser().getEmail() : "";

        return new ApplicationDetailResponse(
                application.getId(),
                application.getStatus(),
                problem.getTitle(),
                problem.getDescription(),
                ngoName,
                applicantName,
                applicantEmail
        );
    }

    public List<ApplicationResponse> getApplicationsByContributor(UUID contributorProfileId) {
        ContributorProfile contributor = contributorProfileRepository.findById(contributorProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Contributor profile not found"));
        String applicantName = contributor.getFirstName() + " " + contributor.getLastName();
        String applicantEmail = (contributor.getUser() != null) ? contributor.getUser().getEmail() : "";
        String applicantSkills = contributor.getSkillsSummary();

        return applicationRepository.findByContributorProfileId(contributorProfileId).stream()
                .map(app -> {
                    ProblemStatement problem = problemStatementRepository.findById(app.getProblemId())
                            .orElse(null);
                    String problemTitle = (problem != null) ? problem.getTitle() : "Unknown Problem";
                    String ngoName = (problem != null && problem.getNgoProfile() != null) 
                            ? problem.getNgoProfile().getOrganizationName() : "Unknown NGO";
                    return new ApplicationResponse(
                            app.getId(),
                            app.getProblemId(),
                            app.getContributorProfileId(),
                            app.getStatus(),
                            problemTitle,
                            ngoName,
                            applicantName,
                            applicantEmail,
                            applicantSkills
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ApplicationResponse> getApplicationsForProblem(UUID problemId) {
        ProblemStatement problem = problemStatementRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem statement not found"));
        String problemTitle = problem.getTitle();
        String ngoName = (problem.getNgoProfile() != null) ? problem.getNgoProfile().getOrganizationName() : "Unknown NGO";

        return applicationRepository.findByProblemId(problemId).stream()
                .map(app -> {
                    ContributorProfile contributor = contributorProfileRepository.findById(app.getContributorProfileId())
                            .orElse(null);
                    String applicantName = (contributor != null) ? contributor.getFirstName() + " " + contributor.getLastName() : "Unknown Contributor";
                    String applicantEmail = (contributor != null && contributor.getUser() != null) ? contributor.getUser().getEmail() : "";
                    String applicantSkills = (contributor != null) ? contributor.getSkillsSummary() : "";
                    return new ApplicationResponse(
                            app.getId(),
                            app.getProblemId(),
                            app.getContributorProfileId(),
                            app.getStatus(),
                            problemTitle,
                            ngoName,
                            applicantName,
                            applicantEmail,
                            applicantSkills
                    );
                })
                .collect(java.util.stream.Collectors.toList());
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
        Application savedApplication = applicationRepository.save(application);

        // 5. If application is accepted, update problem status to IN_PROGRESS and auto-reject others
        if ("ACCEPTED".equalsIgnoreCase(request.status())) {
            problem.setStatus(ProblemStatement.Status.IN_PROGRESS);
            problemStatementRepository.save(problem);

            List<Application> allAppsForProblem = applicationRepository.findByProblemId(problem.getId());
            for (Application app : allAppsForProblem) {
                if (!app.getId().equals(applicationId) && "PENDING".equalsIgnoreCase(app.getStatus())) {
                    app.setStatus("REJECTED");
                    applicationRepository.save(app);
                }
            }
        }

        return savedApplication;
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

        // Update problem statement status to CLOSED
        problem.setStatus(ProblemStatement.Status.CLOSED);
        problemStatementRepository.save(problem);

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