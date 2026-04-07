package com.connectingdots.service;

import com.connectingdots.domain.Role;
import com.connectingdots.domain.User;
import com.connectingdots.domain.UserRepository;
import com.connectingdots.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public record RegisterRequest(String name, String email, String password, String role) {}
    public record AuthResponse(String token, String email, String name, String role, String id) {}

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }
        Role role = Role.valueOf(request.role().toUpperCase());
        User user = new User(null, request.email(),
                passwordEncoder.encode(request.password()),
                request.name(), role);
        userRepository.save(user);
        String token = jwtService.generateToken(user, Map.of("role", role.name(), "name", user.getName()));
        return new AuthResponse(token, user.getEmail(), user.getName(), role.name(), user.getId().toString());
    }

    public AuthResponse login(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String token = jwtService.generateToken(user,
                Map.of("role", user.getRole().name(), "name", user.getName()));
        return new AuthResponse(token, user.getEmail(), user.getName(),
                user.getRole().name(), user.getId().toString());
    }
}
