package com.connectingdots.core_service.security;

import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserRepository userRepository;

    // 1. UserDetailsService: Tells Spring how to fetch users from our DB and
    // translates
    // our custom User entity into a standard Spring Security User object.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            com.connectingdots.core_service.entity.User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found in database"));

            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole().name())
                    .build();
        };
    }

    // 2. PasswordEncoder: The industry-standard BCrypt algorithm used to hash
    // passwords.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 3. AuthenticationProvider: The engine that compares the password the user
    // typed
    // against the hashed password fetched from the database.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Pass the userDetailsService directly into the constructor!
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 4. AuthenticationManager: The top-level manager we will use later in our
    // AuthController to actually log people in.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 5. SecurityFilterChain: The actual rules for our API endpoints!
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because we use stateless JWTs, not browser cookies
                .csrf(AbstractHttpConfigurer::disable)

                // Define our URL access rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/core/auth/**").permitAll() // The future login/register routes are public
                        .requestMatchers(HttpMethod.PUT, "/api/v1/core/problem-statements/*/ai-update").permitAll()
                        .requestMatchers("/api/v1/core/ping").permitAll() // Our test ping route is public
                        .anyRequest().authenticated() // Every other request MUST have a valid JWT
                )

                // Tell Spring not to store sessions in memory (enforcing Stateless
                // architecture)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Register our Authentication engine
                .authenticationProvider(authenticationProvider())

                // Put our custom JWT filter in front of the line! It intercepts requests before
                // they reach the controllers.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}