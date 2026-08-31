package com.connectingdots.core_service.dto;

import com.connectingdots.core_service.entity.User.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    private String email;
    private String password;
    private Role role; // NGO or CONTRIBUTOR
    private String fullName;
    private String organizationName;
    private String primaryDomain;

    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Please enter a valid phone number (7 to 15 digits)")
    private String contactNumber;
    private String location;
    private String preferredLanguage;

    // Contributor specific optional fields at registration
    private String title;
    private String skillsSummary;
    private String portfolioUrl;
}