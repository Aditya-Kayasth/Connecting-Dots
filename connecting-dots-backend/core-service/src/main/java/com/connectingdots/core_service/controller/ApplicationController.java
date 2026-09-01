package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ApplicationRequest;
import com.connectingdots.core_service.dto.ApplicationStatusUpdateRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/core/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Application> applyToProblem(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.applyToProblem(request);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<com.connectingdots.core_service.dto.ApplicationDetailResponse> getApplicationDetails(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(applicationService.getApplicationDetails(applicationId));
    }

    @GetMapping("/contributor/{contributorProfileId}")
    public ResponseEntity<List<com.connectingdots.core_service.dto.ApplicationResponse>> getContributorApplications(@PathVariable UUID contributorProfileId) {
        List<com.connectingdots.core_service.dto.ApplicationResponse> applications = applicationService.getApplicationsByContributor(contributorProfileId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/problem/{problemId}")
    public ResponseEntity<List<com.connectingdots.core_service.dto.ApplicationResponse>> getProblemApplications(@PathVariable UUID problemId) {
        List<com.connectingdots.core_service.dto.ApplicationResponse> applications = applicationService.getApplicationsForProblem(problemId);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<Application> updateApplicationStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        Application application = applicationService.updateApplicationStatus(applicationId, request);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{applicationId}/complete")
    public ResponseEntity<Application> completeApplication(@PathVariable UUID applicationId) {
        Application application = applicationService.completeApplication(applicationId);
        return ResponseEntity.ok(application);
    }
}