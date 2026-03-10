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
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(),
                            dto.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return generateTokens(user);
    }

    public AuthResponse register(AuthRegister dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Email already in use");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .profileName(dto.getProfileName())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .isActive(true)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return generateTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        User user = userRepository.findByRefreshTokenHash(
                        hashToken(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
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
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        return new AuthResponse(accessToken, refreshToken);
    }
}