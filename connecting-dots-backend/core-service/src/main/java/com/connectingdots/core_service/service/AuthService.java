package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.AuthRequest;
import com.connectingdots.core_service.dto.AuthResponse;
import com.connectingdots.core_service.dto.RegisterRequest;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.entity.User.Role;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }

        Role role = request.getRole();
        if (role == Role.ADMIN || role == null) {
            role = Role.CONTRIBUTOR; // Force to Contributor to prevent public privilege escalation
        }

        var user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        user = repository.save(user);

        if (role == Role.CONTRIBUTOR) {
            String fullName = request.getFullName() != null && !request.getFullName().isBlank() 
                    ? request.getFullName().trim() : "Contributor";
            String[] parts = fullName.split("\\s+", 2);
            String firstName = parts[0];
            String lastName = parts.length > 1 ? parts[1] : "User";

            String lang = (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank())
                    ? request.getPreferredLanguage() : "en";
            String title = (request.getTitle() != null && !request.getTitle().isBlank())
                    ? request.getTitle().trim() : "Technical Contributor";
            String location = (request.getLocation() != null && !request.getLocation().isBlank())
                    ? request.getLocation().trim() : "Community Member";
            String skills = (request.getSkillsSummary() != null && !request.getSkillsSummary().isBlank())
                    ? request.getSkillsSummary().trim() : "Software Development & Civic Tech";

            ContributorProfile profile = ContributorProfile.builder()
                    .user(user)
                    .firstName(firstName)
                    .lastName(lastName)
                    .skillsSummary(skills)
                    .portfolioUrl(request.getPortfolioUrl())
                    .preferredLanguage(lang)
                    .title(title)
                    .location(location)
                    .contactNumber(request.getContactNumber())
                    .completedProjects(0)
                    .build();

            contributorProfileRepository.save(profile);
        } else if (role == Role.NGO) {
            String orgName = request.getOrganizationName() != null && !request.getOrganizationName().isBlank()
                    ? request.getOrganizationName().trim() : "NGO Organization";
            String domain = request.getPrimaryDomain() != null && !request.getPrimaryDomain().isBlank()
                    ? request.getPrimaryDomain().trim() : "General Social Impact";
            String lang = (request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank())
                    ? request.getPreferredLanguage() : "en";
            String location = (request.getLocation() != null && !request.getLocation().isBlank())
                    ? request.getLocation().trim() : "Global Community";

            NgoProfile profile = NgoProfile.builder()
                    .user(user)
                    .organizationName(orgName)
                    .domain(domain)
                    .contactNumber(request.getContactNumber())
                    .location(location)
                    .preferredLanguage(lang)
                    .isVerified(false)
                    .build();

            ngoProfileRepository.save(profile);
        }

        var jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found in database."));

        var jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }
}