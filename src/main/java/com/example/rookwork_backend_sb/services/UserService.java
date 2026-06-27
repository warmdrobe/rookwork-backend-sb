package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.user.UpdateNotificationsRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePasswordRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePreferencesRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdateProfileRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

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
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = getUserOrThrow(userId);

        // Google auth users might not have a password
        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Incorrect current password");
            }
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void deleteAccount(UUID userId, com.example.rookwork_backend_sb.dtos.user.DeleteAccountRequest request) {
        User user = getUserOrThrow(userId);
        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Incorrect password");
            }
        }
        userRepository.delete(user);
    }
}
