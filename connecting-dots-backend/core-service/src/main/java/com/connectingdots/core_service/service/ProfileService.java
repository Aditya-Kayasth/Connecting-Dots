package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ContributorProfileRequest;
import com.connectingdots.core_service.dto.ContributorProfileUpdateRequest;
import com.connectingdots.core_service.dto.NgoProfileRequest;
import com.connectingdots.core_service.dto.NgoProfileUpdateRequest;
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
        String loc = (request.location() != null && !request.location().isBlank()) ? request.location() : "Global Community";

        NgoProfile profile = NgoProfile.builder()
                .user(user)
                .organizationName(request.organizationName())
                .domain(request.domain())
                .contactNumber(request.contactNumber())
                .preferredLanguage(lang)
                .location(loc)
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
        String title = (request.title() != null && !request.title().isBlank()) ? request.title() : "Technical Contributor";
        String location = (request.location() != null && !request.location().isBlank()) ? request.location() : "Community Member";

        ContributorProfile profile = ContributorProfile.builder()
                .user(user)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .skillsSummary(request.skillsSummary())
                .portfolioUrl(request.portfolioUrl())
                .preferredLanguage(lang)
                .title(title)
                .location(location)
                .contactNumber(request.contactNumber())
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

    public NgoProfile getAuthenticatedNgoProfile() {
        User user = getAuthenticatedUser();
        return ngoProfileRepository.findByUser(user)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "NGO profile not found for authenticated user"));
    }

    public ContributorProfile getAuthenticatedContributorProfile() {
        User user = getAuthenticatedUser();
        return contributorProfileRepository.findByUser(user)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Contributor profile not found for authenticated user"));
    }

    @Transactional
    public NgoProfile updateNgoProfile(java.util.UUID id, NgoProfileUpdateRequest request) {
        NgoProfile profile = getNgoProfileById(id);
        
        User user = getAuthenticatedUser();
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin && (profile.getUser() == null || !profile.getUser().getId().equals(user.getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Unauthorized: You do not own this profile.");
        }

        profile.setOrganizationName(request.organizationName());
        profile.setDomain(request.domain());
        profile.setContactNumber(request.contactNumber());
        if (request.preferredLanguage() != null) {
            profile.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.location() != null) {
            profile.setLocation(request.location());
        }
        return ngoProfileRepository.save(profile);
    }

    @Transactional
    public ContributorProfile updateContributorProfile(java.util.UUID id, ContributorProfileUpdateRequest request) {
        ContributorProfile profile = getContributorProfileById(id);

        User user = getAuthenticatedUser();
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin && (profile.getUser() == null || !profile.getUser().getId().equals(user.getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Unauthorized: You do not own this profile.");
        }

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setSkillsSummary(request.skillsSummary());
        profile.setPortfolioUrl(request.portfolioUrl());
        if (request.preferredLanguage() != null) {
            profile.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.title() != null) {
            profile.setTitle(request.title());
        }
        if (request.location() != null) {
            profile.setLocation(request.location());
        }
        if (request.contactNumber() != null) {
            profile.setContactNumber(request.contactNumber());
        }
        return contributorProfileRepository.save(profile);
    }

    @Transactional
    public void deleteAuthenticatedUserAndProfile() {
        User user = getAuthenticatedUser();
        userRepository.delete(user);
    }
}