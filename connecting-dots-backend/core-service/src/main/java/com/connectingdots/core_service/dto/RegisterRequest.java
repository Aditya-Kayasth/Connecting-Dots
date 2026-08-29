package com.connectingdots.core_service.dto;

import com.connectingdots.core_service.entity.User.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
}