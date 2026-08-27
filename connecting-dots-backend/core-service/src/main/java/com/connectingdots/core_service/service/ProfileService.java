package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ContributorProfileRequest;
import com.connectingdots.core_service.dto.NgoProfileRequest;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;

    // A helper method to get the currently logged-in user from the JWT Security Context
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    @Transactional
    public NgoProfile createNgoProfile(NgoProfileRequest request) {
        User user = getAuthenticatedUser();

        // Security check: Only users with the NGO role can create this profile
        if (user.getRole() != User.Role.NGO) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Unauthorized: Only NGOs can create NGO profiles.");
        }

        String lang = (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) ? request.preferredLanguage() : "en";

        NgoProfile profile = NgoProfile.builder()
                .user(user)
                .organizationName(request.organizationName())
                .domain(request.domain())
                .contactNumber(request.contactNumber())
                .preferredLanguage(lang)
                .build();

        return ngoProfileRepository.save(profile);
    }

    @Transactional
    public ContributorProfile createContributorProfile(ContributorProfileRequest request) {
        User user = getAuthenticatedUser();

        if (user.getRole() != User.Role.CONTRIBUTOR) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Unauthorized: Only Contributors can create Contributor profiles.");
        }

        String lang = (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) ? request.preferredLanguage() : "en";

        ContributorProfile profile = ContributorProfile.builder()
                .user(user)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .skillsSummary(request.skillsSummary())
                .portfolioUrl(request.portfolioUrl())
                .preferredLanguage(lang)
                .build();

        return contributorProfileRepository.save(profile);
    }

    public java.util.List<NgoProfile> getAllNgoProfiles() {
        return ngoProfileRepository.findAll();
    }

    public java.util.List<ContributorProfile> getAllContributorProfiles() {
        return contributorProfileRepository.findAll();
    }

    public NgoProfile getNgoProfileById(java.util.UUID id) {
        return ngoProfileRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "NGO profile not found"));
    }

    public ContributorProfile getContributorProfileById(java.util.UUID id) {
        return contributorProfileRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Contributor profile not found"));
    }
}