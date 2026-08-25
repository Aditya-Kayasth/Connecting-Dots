package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.AuthRequest;
import com.connectingdots.core_service.dto.AuthResponse;
import com.connectingdots.core_service.dto.RegisterRequest;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if user already exists to prevent duplicate emails
        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }

        // 2. Build the new User, hashing the password immediately
        var user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        // 3. Save to database
        repository.save(user);

        // 4. Generate the JWT wristband
        var jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        
        return AuthResponse.builder().token(jwtToken).build();
    }

    public AuthResponse login(AuthRequest request) {
        // 1. The AuthenticationManager automatically checks the password against the database hash
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. If we reach here, the password was correct. Fetch the user to get their role.
        var user = repository.findByEmail(request.getEmail()).orElseThrow();

        // 3. Generate a fresh JWT wristband
        var jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        
        return AuthResponse.builder().token(jwtToken).build();
    }
}