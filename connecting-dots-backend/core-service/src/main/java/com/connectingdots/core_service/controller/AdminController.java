package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/core/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ProblemStatementRepository problemStatementRepository;
    private final ApplicationRepository applicationRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;

    @Data
    @Builder
    public static class AdminStatsResponse {
        private long totalUsers;
        private long totalNgos;
        private long totalProblems;
        private long totalApplications;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getPlatformStats() {
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalNgos(ngoProfileRepository.count())
                .totalProblems(problemStatementRepository.count())
                .totalApplications(applicationRepository.count())
                .build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @DeleteMapping("/users/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        String currentEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unauthorized: An admin cannot delete their own account."
            );
        }

        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser != null && targetUser.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                     .filter(u -> u.getRole() == User.Role.ADMIN)
                     .count();
            if (adminCount <= 1) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Unauthorized: Cannot delete the last admin account."
                );
            }
        }

        if (targetUser != null) {
            ngoProfileRepository.findByUser(targetUser).ifPresent(p -> {
                ngoProfileRepository.delete(p);
                ngoProfileRepository.flush();
            });
            contributorProfileRepository.findByUser(targetUser).ifPresent(p -> {
                contributorProfileRepository.delete(p);
                contributorProfileRepository.flush();
            });
            userRepository.delete(targetUser);
            userRepository.flush();
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/problems")
    public ResponseEntity<List<com.connectingdots.core_service.entity.ProblemStatement>> getAllProblems() {
        return ResponseEntity.ok(problemStatementRepository.findAll());
    }

    @DeleteMapping("/problems/{id}")
    public ResponseEntity<Void> deleteProblemStatement(@PathVariable UUID id) {
        if (problemStatementRepository.existsById(id)) {
            problemStatementRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<com.connectingdots.core_service.entity.NgoProfile>> getAllNgoProfiles() {
        return ResponseEntity.ok(ngoProfileRepository.findAll());
    }

    @PutMapping("/ngos/{id}/verify")
    public ResponseEntity<com.connectingdots.core_service.entity.NgoProfile> toggleNgoVerification(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean status) {
        return ngoProfileRepository.findById(id).map(ngo -> {
            boolean newStatus = (status != null) ? status : !ngo.isVerified();
            ngo.setVerified(newStatus);
            return ResponseEntity.ok(ngoProfileRepository.save(ngo));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
