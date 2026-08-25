package com.connectingdots.core_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // 1. Pulls the master secret from your .env file
    @Value("${JWT_SECRET}")
    private String secretString;

    // 2. Defines how long the wristband is valid (e.g., 24 hours)
    private final long jwtExpirationMs = 86400000; 

    // 3. Converts your string secret into a cryptographically secure key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes());
    }

    // 4. The method that generates the token when a user logs in
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email) // The user's identity
                .claim("role", role) // Adding their role (NGO or CONTRIBUTOR)
                .issuedAt(new Date()) // The time it was handed out
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // When it expires
                .signWith(getSigningKey()) // Stamping it with the un-forgeable seal
                .compact();
    }

    // 5. The method the filter will use to extract the user's email from incoming tokens
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 6. The method that verifies the token's cryptographic seal hasn't been tampered with
    public boolean isTokenValid(String token, String userEmail) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(userEmail) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // This is the specific JJWT 0.12.x syntax to securely parse a token
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}