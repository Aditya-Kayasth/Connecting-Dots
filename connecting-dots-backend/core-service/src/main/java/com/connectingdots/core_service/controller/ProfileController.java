package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ContributorProfileRequest;
import com.connectingdots.core_service.dto.NgoProfileRequest;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/core/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/ngo")
    public ResponseEntity<NgoProfile> createNgoProfile(@RequestBody NgoProfileRequest request) {
        NgoProfile createdProfile = profileService.createNgoProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    @PostMapping("/contributor")
    public ResponseEntity<ContributorProfile> createContributorProfile(@RequestBody ContributorProfileRequest request) {
        ContributorProfile createdProfile = profileService.createContributorProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }
}