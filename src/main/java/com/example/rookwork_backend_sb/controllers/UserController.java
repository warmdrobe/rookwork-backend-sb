package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.user.UpdateNotificationsRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePasswordRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdatePreferencesRequest;
import com.example.rookwork_backend_sb.dtos.user.UpdateProfileRequest;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import com.example.rookwork_backend_sb.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing endpoints for user profile operations.
 */
@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
  private final SecurityUtil securityUtil;
  private final UserRepository userRepository;
  private final UserService userService;

  /**
   * Retrieves the profile summary of the currently authenticated user.
   *
   * @return response entity containing the UserSummary DTO of the current user
   * @throws ResourceNotFoundException if user is not found in database
   */
  @GetMapping("/me")
  public ResponseEntity<UserSummary> getCurrentUser() {
    UUID userId = securityUtil.getCurrentUserId();
    User user = userRepository.findById(userId)

        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return ResponseEntity.ok(UserSummary.builder()
        .id(user.getId())
        .profileName(user.getProfileName())
        .email(user.getEmail())
        .picture(user.getPicture())
        .jobTitle(user.getJobTitle())
        .organization(user.getOrganization())
        .location(user.getLocation())
        .emailPublic(user.isEmailPublic())
        .jobTitlePublic(user.isJobTitlePublic())
        .organizationPublic(user.isOrganizationPublic())
        .locationPublic(user.isLocationPublic())
        .notifyIssueAssigned(user.isNotifyIssueAssigned())
        .notifyMentioned(user.isNotifyMentioned())
        .notifyProjectUpdates(user.isNotifyProjectUpdates())
        .notifyDailyDigest(user.isNotifyDailyDigest())
        .build());
  }

  @PutMapping("/me/profile")
  public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
    UUID userId = securityUtil.getCurrentUserId();
    userService.updateProfile(userId, request);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/preferences")
  public ResponseEntity<Void> updatePreferences(@RequestBody UpdatePreferencesRequest request) {
    UUID userId = securityUtil.getCurrentUserId();
    userService.updatePreferences(userId, request);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/notifications")
  public ResponseEntity<Void> updateNotifications(@RequestBody UpdateNotificationsRequest request) {
    UUID userId = securityUtil.getCurrentUserId();
    userService.updateNotifications(userId, request);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/password")
  public ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordRequest request) {
    UUID userId = securityUtil.getCurrentUserId();
    userService.updatePassword(userId, request);
    return ResponseEntity.noContent().build();
  }
}
