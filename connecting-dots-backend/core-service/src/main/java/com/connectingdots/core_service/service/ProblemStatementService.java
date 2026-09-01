package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.IngestionMessage;
import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.dto.TranslationRequest;
import com.connectingdots.core_service.dto.TranslationResponse;
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
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemStatementService {

    private final ProblemStatementRepository problemStatementRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final UserRepository userRepository;
    private final com.connectingdots.core_service.repository.ApplicationRepository applicationRepository;
    private final QStashService qStashService;

    private final RestClient restClient = RestClient.create();

    @Value("${ai.service.url:http://localhost:8082}")
    private String aiServiceUrl;

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

        String rawTitle = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : "Draft Problem Ingesting...";
        String rawDescription = (request.description() != null && !request.description().isBlank())
                ? request.description().trim()
                : "AI is analyzing the instructions/document...";
        String rawDomain = (request.domain() != null && !request.domain().isBlank())
                ? request.domain().trim()
                : "Others";

        // 3. Build and save the new Problem Statement
        ProblemStatement problemStatement = ProblemStatement.builder()
                .ngoProfile(ngoProfile)
                .title(rawTitle)
                .description(rawDescription)
                .domain(rawDomain)
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
                        .uri(aiServiceUrl + "/api/v1/ai/translate")
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
        String rawLang = "en";
        if (user.getRole() == User.Role.NGO) {
            rawLang = ngoProfileRepository.findByUser(user)
                    .map(ngoProfile -> ngoProfile.getPreferredLanguage())
                    .orElse("en");
        } else if (user.getRole() == User.Role.CONTRIBUTOR) {
            rawLang = contributorProfileRepository.findByUser(user)
                    .map(profile -> profile.getPreferredLanguage())
                    .orElse("en");
        }

        if (rawLang == null) {
            return "en";
        }
        String lang = rawLang.trim().toLowerCase();
        if (lang.contains("hindi") || lang.equals("hi")) return "hi";
        if (lang.contains("marathi") || lang.equals("mr")) return "mr";
        if (lang.contains("swahili") || lang.equals("sw")) return "sw";
        return "en";
    }

    @Transactional
    public void updateProblemStatementWithAiResults(java.util.UUID id, com.connectingdots.core_service.dto.AiUpdatePayload payload) {
        ProblemStatement problemStatement = problemStatementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem statement not found"));
        
        if (payload.title() != null && !payload.title().isBlank()) {
            problemStatement.setTitle(payload.title().trim());
        }
        if (payload.description() != null && !payload.description().isBlank()) {
            problemStatement.setDescription(payload.description().trim());
        }
        if (payload.domain() != null && !payload.domain().isBlank()) {
            problemStatement.setDomain(payload.domain().trim());
        }
        
        if (payload.status() != null && !payload.status().trim().isEmpty()) {
            try {
                problemStatement.setStatus(ProblemStatement.Status.valueOf(payload.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                problemStatement.setStatus(ProblemStatement.Status.PROCESSED);
            }
        } else {
            problemStatement.setStatus(ProblemStatement.Status.PROCESSED);
        }
        
        problemStatementRepository.save(problemStatement);
    }

    @Transactional
    public void triggerIngestion(UUID id) {
        User user = getAuthenticatedUser();
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated.");
        }

        ProblemStatement problem = problemStatementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found"));

        // If not an admin, verify the caller owns the problem statement
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin) {
            if (user.getRole() != User.Role.NGO) {
                throw new SecurityException("Only the owner NGO can trigger ingestion.");
            }
            NgoProfile ngoProfile = ngoProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"));
            if (problem.getNgoProfile() == null || !problem.getNgoProfile().getId().equals(ngoProfile.getId())) {
                throw new SecurityException("You do not own this problem statement.");
            }
        }

        IngestionMessage message = new IngestionMessage(
                problem.getId(),
                problem.getSourceFileUrl(),
                problem.getSourceType()
        );

        qStashService.publishToAiService(message);

        problem.setStatus(ProblemStatement.Status.PROCESSING);
        problemStatementRepository.save(problem);
    }

    @Transactional
    public ProblemStatement updateProblemStatement(UUID id, ProblemStatementRequest request) {
        ProblemStatement problem = problemStatementRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Problem statement not found"));

        User user = getAuthenticatedUser();
        if (user != null && user.getRole() != User.Role.ADMIN) {
            NgoProfile ngoProfile = ngoProfileRepository.findByUser(user).orElse(null);
            if (ngoProfile == null || problem.getNgoProfile() == null || !problem.getNgoProfile().getId().equals(ngoProfile.getId())) {
                throw new SecurityException("You are not authorized to update this problem statement");
            }
        }

        if (request.title() != null && !request.title().isBlank()) {
            problem.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            problem.setDescription(request.description().trim());
        }
        if (request.domain() != null && !request.domain().isBlank()) {
            problem.setDomain(request.domain().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            try {
                problem.setStatus(ProblemStatement.Status.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // keep existing status if invalid string passed
            }
        }

        return problemStatementRepository.save(problem);
    }

    @Transactional
    public void deleteProblemStatement(UUID id) {
        ProblemStatement problem = problemStatementRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Problem statement not found"));

        User user = getAuthenticatedUser();
        if (user != null && user.getRole() != User.Role.ADMIN) {
            NgoProfile ngoProfile = ngoProfileRepository.findByUser(user).orElse(null);
            if (ngoProfile == null || problem.getNgoProfile() == null || !problem.getNgoProfile().getId().equals(ngoProfile.getId())) {
                throw new SecurityException("You are not authorized to delete this problem statement");
            }
        }

        // Delete associated applications first
        applicationRepository.deleteByProblemId(id);
        problemStatementRepository.delete(problem);
    }
}