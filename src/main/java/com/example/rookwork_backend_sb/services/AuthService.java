package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.auth.AuthRegister;
import com.example.rookwork_backend_sb.dtos.auth.AuthResponse;
import com.example.rookwork_backend_sb.dtos.auth.LoginRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.AppException;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service class handling user authentication, registration, and token refresh logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user with email and password, and returns access and refresh tokens.
     *
     * @param dto the login request credentials
     * @return the authentication response containing tokens
     * @throws UnauthorizedException if credentials are invalid
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public AuthResponse login(LoginRequest dto) {
        try {
            // Authenticate user credentials via Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(),
                            dto.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Fetch user from repository and generate authentication tokens
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return generateTokens(user);
    }

    /**
     * Registers a new user account if the email is not already in use.
     *
     * @param dto the registration details
     * @return the authentication response containing tokens for the new user
     * @throws ConflictException if the email is already registered
     */
    public AuthResponse register(AuthRegister dto) {
        // Prevent duplicate user registrations with the same email
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Email already in use");
        }

        // Build and persist the new active user entity with hashed password
        User user = User.builder()
                .email(dto.getEmail())
                .profileName(dto.getProfileName())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .isActive(true)
                .isVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(user);

        return generateTokens(user);
    }

    /**
     * Refreshes and returns new access/refresh tokens using a valid refresh token.
     *
     * @param refreshToken the plaintext refresh token
     * @return the authentication response containing new tokens
     * @throws UnauthorizedException if the token is invalid or expired
     */
    public AuthResponse refresh(String refreshToken) {
        // Retrieve the user matching the hashed refresh token
        User user = userRepository.findByRefreshTokenHash(
                        hashToken(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Validate that the refresh token is still within its expiration window
        if (user.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        return generateTokens(user);
    }

    // Helper
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String result = hexString.toString();
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,"Error hashing token");
        }
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtService.generateToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken();
        user.setRefreshTokenHash(hashToken(refreshToken));
        user.setRefreshTokenExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);
        return new AuthResponse(accessToken, refreshToken);
    }
}