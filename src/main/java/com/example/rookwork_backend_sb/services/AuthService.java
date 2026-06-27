package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.auth.AuthRegister;
import com.example.rookwork_backend_sb.dtos.auth.AuthResponse;
import com.example.rookwork_backend_sb.dtos.auth.LoginRequest;
import com.example.rookwork_backend_sb.dtos.auth.GoogleLoginRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.AppException;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Service class handling user authentication, registration, and token refresh
 * logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.client-id:#{null}}")
    private String googleClientId;

    /**
     * Authenticates a user with email and password, and returns access and refresh
     * tokens.
     *
     * @param dto the login request credentials
     * @return the authentication response containing tokens
     * @throws UnauthorizedException     if credentials are invalid
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public AuthResponse login(LoginRequest dto) {
        try {
            // Authenticate user credentials via Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(),
                            dto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Fetch user from repository and generate authentication tokens
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return generateTokens(user);
    }

    public boolean checkEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Registers a new user account if the email is not already in use.
     *
     * @param dto the registration details
     * @return the authentication response containing tokens for the new user
     * @throws ConflictException if the email is already registered or invalid
     *                           format
     */
    public AuthResponse register(AuthRegister dto) {
        if (!dto.getEmail().matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            throw new ConflictException("Chỉ chấp nhận email định dạng @gmail.com");
        }

        // Prevent duplicate user registrations with the same email
        if (checkEmailExists(dto.getEmail())) {
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
        emailService.sendWelcomeEmail(user.getEmail(), user.getProfileName());

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
        if (user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
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
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            String result = hexString.toString();
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Error hashing token");
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

    public AuthResponse googleLogin(GoogleLoginRequest dto) {
        Map<String, Object> payload = verifyGoogleToken(dto.getToken());

        String email = (String) payload.get("email");
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (email == null || email.trim().isEmpty()) {
            throw new UnauthorizedException("Google token does not contain email");
        }

        boolean isNewUser = false;
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            isNewUser = true;
            user = User.builder()
                    .email(email)
                    .profileName(name != null ? name : email.split("@")[0])
                    .picture(picture)
                    .passwordHash(null)
                    .isActive(true)
                    .isVerified(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            user = userRepository.save(user);
        }

        boolean updated = false;
        // Always sync the latest Google avatar URL on each login
        if (picture != null && !picture.equals(user.getPicture())) {
            user.setPicture(picture);
            updated = true;
        }
        if ((user.getProfileName() == null || user.getProfileName().isEmpty()) && name != null) {
            user.setProfileName(name);
            updated = true;
        }
        if (updated) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }

        if (isNewUser) {
            emailService.sendWelcomeEmail(user.getEmail(), user.getProfileName());
        }
        return generateTokens(user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new UnauthorizedException("Invalid Google token");
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);

            if (googleClientId != null && !googleClientId.trim().isEmpty()) {
                String aud = (String) payload.get("aud");
                if (!googleClientId.equals(aud)) {
                    throw new UnauthorizedException("Google token audience mismatch");
                }
            }

            return payload;
        } catch (Exception e) {


            if (e instanceof UnauthorizedException) {
                throw (UnauthorizedException) e;
            }
            throw new UnauthorizedException("Failed to verify Google token: " + e.getMessage());
        }
    }
}
