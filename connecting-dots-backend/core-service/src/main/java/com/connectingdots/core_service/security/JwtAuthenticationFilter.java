package com.connectingdots.core_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Look for the "Authorization" header in the incoming request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. If there is no header, or it doesn't start with "Bearer ", let it pass
        // (It might be a public endpoint like /login, which the next layer will handle)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the actual token string (removing the "Bearer " prefix)
        jwt = authHeader.substring(7);

        try {
            userEmail = jwtUtil.extractEmail(jwt);

            // 4. If we found an email, and the user isn't already authenticated in this
            // thread...
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Verify the token hasn't been forged or expired
                if (jwtUtil.isTokenValid(jwt, userEmail)) {

                    List<GrantedAuthority> authorities = new ArrayList<>();
                    String role = jwtUtil.extractRole(jwt);
                    if (role != null && !role.isBlank()) {
                        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                        authorities.add(new SimpleGrantedAuthority(roleName));
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. Officially log the user into the Spring Security Context for this single
                    // request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If the token is malformed or expired, do nothing. Spring Security will block
            // them later.
        }

        // 7. Pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}