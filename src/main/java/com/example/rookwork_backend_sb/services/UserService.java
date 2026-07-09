package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.user.UpdateNotificationsRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePasswordRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePreferencesRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdateProfileRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final EmailService emailService;

    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        if (request.getProfileName() != null) {
            user.setProfileName(request.getProfileName());
        }
        if (request.getPicture() != null) {
            user.setPicture(request.getPicture());
        }
        if (request.getJobTitle() != null) {
            user.setJobTitle(request.getJobTitle());
        }
        if (request.getOrganization() != null) {
            user.setOrganization(request.getOrganization());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getEmailPublic() != null) {
            user.setEmailPublic(request.getEmailPublic());
        }
        if (request.getJobTitlePublic() != null) {
            user.setJobTitlePublic(request.getJobTitlePublic());
        }
        if (request.getOrganizationPublic() != null) {
            user.setOrganizationPublic(request.getOrganizationPublic());
        }
        if (request.getLocationPublic() != null) {
            user.setLocationPublic(request.getLocationPublic());
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) throws IOException {
        User user = getUserOrThrow(userId);

        // 1. Upload to S3
        String storedName = s3Service.uploadAvatar(file, userId);

        // 2. Track old picture to clean up
        String oldPicture = user.getPicture();

        // 3. Update user profile picture
        user.setPicture(storedName);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // 4. Clean up old S3 avatar if exists
        if (oldPicture != null && oldPicture.startsWith("avatar/")) {
            try {
                s3Service.deleteFile(oldPicture);
            } catch (Exception e) {
                // Ignore error during cleanup
            }
        }

        // 5. Return presigned URL
        return s3Service.getAvatarUrl(storedName);
    }

    @Transactional
    public void deleteAvatar(UUID userId) {
        User user = getUserOrThrow(userId);
        String oldPicture = user.getPicture();

        // 1. Reset picture to null in database
        user.setPicture(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // 2. Clean up S3 file if it was an uploaded avatar
        if (oldPicture != null && oldPicture.startsWith("avatar/")) {
            try {
                s3Service.deleteFile(oldPicture);
            } catch (Exception e) {
                // Ignore error during S3 cleanup
            }
        }
    }

    public void updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = getUserOrThrow(userId);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public void updateNotifications(UUID userId, UpdateNotificationsRequest request) {
        User user = getUserOrThrow(userId);
        user.setNotifyIssueAssigned(request.isNotifyIssueAssigned());
        user.setNotifyMentioned(request.isNotifyMentioned());
        user.setNotifyProjectUpdates(request.isNotifyProjectUpdates());
        user.setNotifyDailyDigest(request.isNotifyDailyDigest());
        user.setNotifyComment(request.isNotifyComment());
        user.setNotifyEventInvited(request.isNotifyEventInvited());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public void requestPasswordOtp(UUID userId) {
        User user = getUserOrThrow(userId);

        if (user.getPasswordHash() == null) {
            throw new BadRequestException("Google accounts without password can set a password directly without OTP");
        }

        // Check password change frequency limit (2 times/month)
        if (isPasswordLimitReached(user)) {
            throw new BadRequestException("You can only change your password up to 2 times per month");
        }

        if (user.getOtpExpiry() != null) {
            Instant lastSent = user.getOtpExpiry().minus(5, ChronoUnit.MINUTES);
            if (Instant.now().isBefore(lastSent.plus(1, ChronoUnit.MINUTES))) {
                throw new BadRequestException("Please wait at least 1 minute before requesting another OTP");
            }
        }

        java.security.SecureRandom random = new java.security.SecureRandom();
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        user.setOtpCode(otpCode);
        user.setOtpExpiry(Instant.now().plus(5, ChronoUnit.MINUTES));
        user.setOtpFailedAttempts(0);
        userRepository.save(user);

        emailService.sendPasswordResetOtpEmail(user.getEmail(), otpCode);
    }

    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = getUserOrThrow(userId);

        // Check password change frequency limit (2 times/month)
        if (isPasswordLimitReached(user)) {
            throw new BadRequestException("You can only change your password up to 2 times per month");
        }
        user.setPasswordChangesThisMonth(getPasswordChangesThisMonthCalculated(user) + 1);

        if (user.getPasswordHash() != null) {
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                throw new BadRequestException("OTP verification code is required");
            }

            if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
                throw new BadRequestException("No OTP request found. Please request an OTP code first");
            }

            if (user.getOtpExpiry().isBefore(Instant.now())) {
                throw new BadRequestException("OTP code has expired. Please request a new one");
            }

            if (user.getOtpFailedAttempts() >= 5) {
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                user.setOtpFailedAttempts(0);
                userRepository.save(user);
                throw new BadRequestException("Too many failed OTP attempts. Please request a new OTP code");
            }

            if (!user.getOtpCode().equals(request.getOtp())) {
                user.setOtpFailedAttempts(user.getOtpFailedAttempts() + 1);
                userRepository.save(user);
                throw new BadRequestException("Incorrect OTP verification code");
            }
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpFailedAttempts(0);

        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);

        user.setLastPasswordChangeAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BadRequestException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BadRequestException("Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BadRequestException("Password must contain at least one special character");
        }
    }

    public int getPasswordChangesThisMonthCalculated(User user) {
        if (user.getLastPasswordChangeAt() == null) {
            return 0;
        }
        java.time.ZonedDateTime nowZoned = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime lastZoned = user.getLastPasswordChangeAt().atZone(java.time.ZoneOffset.UTC);
        if (nowZoned.getYear() == lastZoned.getYear() && nowZoned.getMonthValue() == lastZoned.getMonthValue()) {
            return user.getPasswordChangesThisMonth();
        }
        return 0;
    }

    public boolean isPasswordLimitReached(User user) {
        return getPasswordChangesThisMonthCalculated(user) >= 2;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
