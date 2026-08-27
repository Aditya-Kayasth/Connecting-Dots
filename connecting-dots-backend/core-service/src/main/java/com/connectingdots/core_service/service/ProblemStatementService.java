package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.dto.TranslationRequest;
import com.connectingdots.core_service.dto.TranslationResponse;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.connectingdots.core_service.repository.ProblemStatementSpecs;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemStatementService {

    private final ProblemStatementRepository problemStatementRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final UserRepository userRepository;

    private final RestClient restClient = RestClient.create();

    private User getAuthenticatedUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public ProblemStatement createProblemStatement(ProblemStatementRequest request) {
        User user = getAuthenticatedUser();
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        // 1. Verify the user is an NGO
        if (user.getRole() != User.Role.NGO) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, 
                    "Only NGOs can post problem statements."
            );
        }

        // 2. Fetch their specific NGO Profile
        NgoProfile ngoProfile = ngoProfileRepository.findByUser(user)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, 
                        "NGO Profile must be created before posting a problem statement."
                ));

        ProblemStatement.Status resolvedStatus = ProblemStatement.Status.OPEN;
        if (request.status() != null && !request.status().trim().isEmpty()) {
            try {
                resolvedStatus = ProblemStatement.Status.valueOf(request.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                // fallback to OPEN or UPLOADED
                resolvedStatus = (request.sourceFileUrl() != null) ? ProblemStatement.Status.UPLOADED : ProblemStatement.Status.OPEN;
            }
        } else if (request.sourceFileUrl() != null && !request.sourceFileUrl().trim().isEmpty()) {
            resolvedStatus = ProblemStatement.Status.UPLOADED;
        }

        // 3. Build and save the new Problem Statement
        ProblemStatement problemStatement = ProblemStatement.builder()
                .ngoProfile(ngoProfile)
                .title(request.title())
                .description(request.description())
                .domain(request.domain())
                .sourceFileUrl(request.sourceFileUrl())
                .sourceType(request.sourceType())
                .status(resolvedStatus)
                .build();

        return problemStatementRepository.save(problemStatement);
    }

    @Transactional(readOnly = true)
    public Page<ProblemStatement> getProblemStatements(String domain, String status, Pageable pageable) {
        Specification<ProblemStatement> spec = Specification
                .where(ProblemStatementSpecs.hasDomain(domain))
                .and(ProblemStatementSpecs.hasStatus(status));
        return problemStatementRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public ProblemStatement getProblemStatementById(UUID id, String targetLang) {
        ProblemStatement problem = problemStatementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem statement not found"));

        String lang = targetLang;
        if (lang == null || lang.isBlank()) {
            lang = getUserPreferredLanguage();
        }

        if (lang != null && !"en".equalsIgnoreCase(lang)) {
            try {
                TranslationResponse response = restClient.post()
                        .uri("http://localhost:8082/api/v1/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new TranslationRequest(problem.getTitle(), problem.getDescription(), lang))
                        .retrieve()
                        .body(TranslationResponse.class);

                if (response != null && response.translatedTitle() != null) {
                    problem.setTitle(response.translatedTitle());
                    problem.setDescription(response.translatedDescription());
                }
            } catch (Exception e) {
                // Fallback gracefully to original problem statement if AI translation is unavailable
            }
        }

        return problem;
    }

    private String getUserPreferredLanguage() {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "en";
        }
        if (user.getRole() == User.Role.NGO) {
            return ngoProfileRepository.findByUser(user)
                    .map(NgoProfile::getPreferredLanguage)
                    .orElse("en");
        } else if (user.getRole() == User.Role.CONTRIBUTOR) {
            return contributorProfileRepository.findByUser(user)
                    .map(ContributorProfile::getPreferredLanguage)
                    .orElse("en");
        }
        return "en";
    }

    @Transactional
    public void updateProblemStatementWithAiResults(java.util.UUID id, com.connectingdots.core_service.dto.AiUpdatePayload payload) {
        ProblemStatement problemStatement = problemStatementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem statement not found"));
        
        problemStatement.setTitle(payload.title());
        problemStatement.setDescription(payload.description());
        problemStatement.setDomain(payload.domain());
        problemStatement.setStatus(ProblemStatement.Status.PROCESSED);
        
        problemStatementRepository.save(problemStatement);
    }
}