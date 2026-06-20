package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service class responsible for generating, signing, and parsing JSON Web Tokens (JWT) and refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token for a user.
     *
     * @param userId the unique identifier of the user
     * @return the generated JWT access token string
     */
    public String generateToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSignKey())
                .compact();
    }

    /**
     * Extracts the user ID (subject) from a given JWT access token.
     *
     * @param token the JWT access token string
     * @return the extracted user ID as a string representation of UUID
     */
    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Generates a unique, secure refresh token.
     *
     * @return a random UUID string representing the refresh token
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}