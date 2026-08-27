package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ContributorProfileRequest;
import com.connectingdots.core_service.dto.NgoProfileRequest;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/core/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/ngo")
    public ResponseEntity<NgoProfile> createNgoProfile(@Valid @RequestBody NgoProfileRequest request) {
        NgoProfile createdProfile = profileService.createNgoProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    @PostMapping("/contributor")
    public ResponseEntity<ContributorProfile> createContributorProfile(@Valid @RequestBody ContributorProfileRequest request) {
        ContributorProfile createdProfile = profileService.createContributorProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<NgoProfile>> getAllNgoProfiles() {
        return ResponseEntity.ok(profileService.getAllNgoProfiles());
    }

    @GetMapping("/contributors")
    public ResponseEntity<List<ContributorProfile>> getAllContributorProfiles() {
        return ResponseEntity.ok(profileService.getAllContributorProfiles());
    }

    @GetMapping("/ngo/{id}")
    public ResponseEntity<NgoProfile> getNgoProfileById(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getNgoProfileById(id));
    }

    @GetMapping("/contributor/{id}")
    public ResponseEntity<ContributorProfile> getContributorProfileById(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getContributorProfileById(id));
    }
}