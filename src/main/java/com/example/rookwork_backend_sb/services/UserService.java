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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                throw new UnauthorizedException("Incorrect current password");
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
}
