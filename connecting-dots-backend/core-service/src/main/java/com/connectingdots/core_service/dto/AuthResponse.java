package com.connectingdots.core_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token; // This is the JWT wristband we send back!
    private String role;  // User role (ADMIN, NGO, CONTRIBUTOR)
    private java.util.UUID userId; // Add the unique user database identifier

    public AuthResponse(String token) {
        this.token = token;
        this.role = null;
        this.userId = null;
    }
}