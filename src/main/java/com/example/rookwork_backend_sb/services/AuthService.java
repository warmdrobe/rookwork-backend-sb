package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.auth.AuthRegister;
import com.example.rookwork_backend_sb.dtos.auth.AuthResponse;
import com.example.rookwork_backend_sb.dtos.auth.LoginRequest;
import com.example.rookwork_backend_sb.dtos.auth.RegisterResponse;
import com.example.rookwork_backend_sb.dtos.auth.VerifyOtpRequest;
import com.example.rookwork_backend_sb.dtos.auth.GoogleLoginRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.entities.Invitation;
import com.example.rookwork_backend_sb.entities.InvitationStatus;
import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.entities.ProjectRole;
import com.example.rookwork_backend_sb.exceptions.AppException;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.repositories.InvitationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
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
import java.util.List;
import java.util.UUID;

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
    private final InvitationRepository invitationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InvitationService invitationService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.client-id:#{null}}")
    private String googleClientId;

    /**
     * Authenticates a user with email and password, and returns access and refresh tokens.
     *
     * @param dto the login request credentials
     * @return the authentication response containing tokens
     * @throws UnauthorizedException if credentials are invalid
     * @throws ResourceNotFoundException if user doesn't exist
     */
    public AuthResponse login(LoginRequest dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account has not been activated via OTP");
        }

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

        return generateTokens(user);
    }

    public boolean checkEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::isActive)
                .orElse(false);
    }

    /**
     * Registers a new user account if the email is not already in use.
     *
     * @param dto the registration details
     * @return the authentication response containing tokens for the new user
     * @throws ConflictException if the email is already registered
     */
    private String generateOtp() {
        java.util.Random random = new java.util.Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public RegisterResponse register(AuthRegister dto) {
        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(dto.getEmail());
        User user;
        String otp = generateOtp();
        Instant expiry = Instant.now().plus(5, ChronoUnit.MINUTES);

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isVerified()) {
                throw new ConflictException("Email already in use");
            }

            // Prevent password/profile hijacking
            if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
                if (user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(Instant.now())) {
                    throw new ConflictException("A registration is already pending for this email. Please verify the OTP or wait for it to expire.");
                }
            }

            // Cooldown check for re-registering (limit to 1 minute between OTP requests)
            if (user.getOtpExpiry() != null) {
                Instant lastSent = user.getOtpExpiry().minus(5, ChronoUnit.MINUTES);
                Instant cooldownEnd = lastSent.plus(1, ChronoUnit.MINUTES);
                if (Instant.now().isBefore(cooldownEnd)) {
                    long secondsRemaining = ChronoUnit.SECONDS.between(Instant.now(), cooldownEnd);
                    throw new BadRequestException("Please wait " + secondsRemaining + " seconds before registering again.");
                }
            }

            // Update the placeholder user's registration fields but keep it inactive
            user.setProfileName(dto.getProfileName());
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            user.setActive(false);
            user.setVerified(false);
            user.setOtpCode(otp);
            user.setOtpExpiry(expiry);
            user.setOtpFailedAttempts(0);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        } else {
            // Build and persist the new inactive user entity with OTP
            user = User.builder()
                    .email(dto.getEmail())
                    .profileName(dto.getProfileName())
                    .passwordHash(passwordEncoder.encode(dto.getPassword()))
                    .isActive(false)
                    .isVerified(false)
                    .otpCode(otp)
                    .otpExpiry(expiry)
                    .otpFailedAttempts(0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            userRepository.save(user);
        }

        // Send OTP email
        emailService.sendOtpEmail(user.getEmail(), otp);

        return new RegisterResponse(user.getEmail(), "An OTP code has been sent to your email. Please verify.");
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new ConflictException("User is already verified");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("OTP code has expired");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(dto.getOtp())) {
            int attempts = user.getOtpFailedAttempts() + 1;
            user.setOtpFailedAttempts(attempts);
            if (attempts >= 5) {
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                user.setOtpFailedAttempts(0);
                userRepository.save(user);
                throw new BadRequestException("Too many failed OTP attempts. A new OTP must be requested.");
            } else {
                userRepository.save(user);
                throw new BadRequestException("Invalid OTP code. Remaining attempts: " + (5 - attempts));
            }
        }

        // Activate and verify the user
        user.setActive(true);
        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpFailedAttempts(0);
        userRepository.save(user);

        // Auto-join to projects they were invited to
        if (dto.getInvitationId() != null && !dto.getInvitationId().isEmpty()) {
            invitationRepository.findById(UUID.fromString(dto.getInvitationId())).ifPresent(invite -> {
                if (invite.getStatus() == InvitationStatus.PENDING && invite.getInvitedUser().getId().equals(user.getId())) {
                    invite.setStatus(InvitationStatus.ACCEPTED);
                    invite.setUpdatedAt(Instant.now());
                    invitationRepository.save(invite);

                    ProjectMember member = ProjectMember.builder()
                            .id(new ProjectMemberId(user.getId(), invite.getProject().getId()))
                            .user(user)
                            .project(invite.getProject())
                            .role(ProjectRole.CONTRIBUTOR)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    projectMemberRepository.save(member);
                    invitationService.notifyProjectMembersNewMember(user, invite.getProject());
                }
            });
        } else {
            List<Invitation> pendingInvites = invitationRepository.findByInvitedUserIdAndStatus(user.getId(), InvitationStatus.PENDING);
            for (Invitation invite : pendingInvites) {
                invite.setStatus(InvitationStatus.ACCEPTED);
                invite.setUpdatedAt(Instant.now());
                invitationRepository.save(invite);

                ProjectMember member = ProjectMember.builder()
                        .id(new ProjectMemberId(user.getId(), invite.getProject().getId()))
                        .user(user)
                        .project(invite.getProject())
                        .role(ProjectRole.CONTRIBUTOR)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                projectMemberRepository.save(member);
                invitationService.notifyProjectMembersNewMember(user, invite.getProject());
            }
        }

        return generateTokens(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new ConflictException("User is already verified");
        }

        // Cooldown check for resending OTP (limit to 1 minute between OTP requests)
        if (user.getOtpExpiry() != null) {
            Instant lastSent = user.getOtpExpiry().minus(5, ChronoUnit.MINUTES);
            Instant cooldownEnd = lastSent.plus(1, ChronoUnit.MINUTES);
            if (Instant.now().isBefore(cooldownEnd)) {
                long secondsRemaining = ChronoUnit.SECONDS.between(Instant.now(), cooldownEnd);
                throw new BadRequestException("Please wait " + secondsRemaining + " seconds before requesting another OTP.");
            }
        }

        String otp = generateOtp();
        Instant expiry = Instant.now().plus(5, ChronoUnit.MINUTES);

        user.setOtpCode(otp);
        user.setOtpExpiry(expiry);
        user.setOtpFailedAttempts(0);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
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

    public AuthResponse googleLogin(GoogleLoginRequest dto) {
        Map<String, Object> payload = verifyGoogleToken(dto.getToken());

        String email = (String) payload.get("email");
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (email == null || email.trim().isEmpty()) {
            throw new UnauthorizedException("Google token does not contain email");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .profileName(name != null ? name : email.split("@")[0])
                    .picture(picture)
                    .passwordHash(null)
                    .isActive(true)
                    .isVerified(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return userRepository.save(newUser);
        });

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
