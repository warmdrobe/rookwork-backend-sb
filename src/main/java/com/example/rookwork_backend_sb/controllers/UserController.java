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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.example.rookwork_backend_sb.services.S3Service;
import java.io.IOException;

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
  private final S3Service s3Service;

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
        .picture(s3Service.getAvatarUrl(user.getPicture()))
        .jobTitle(user.getJobTitle())
        .language(user.getLanguage())
        .timezone(user.getTimezone())
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

  @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<java.util.Map<String, String>> uploadAvatar(
      @RequestParam("file") MultipartFile file) throws IOException {
    UUID userId = securityUtil.getCurrentUserId();
    String avatarUrl = userService.uploadAvatar(userId, file);
    return ResponseEntity.ok(java.util.Map.of("avatarUrl", avatarUrl));
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

  @org.springframework.web.bind.annotation.DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(@RequestBody com.example.rookwork_backend_sb.dtos.user.DeleteAccountRequest request) {
    UUID userId = securityUtil.getCurrentUserId();
    userService.deleteAccount(userId, request);
    return ResponseEntity.noContent().build();
  }
}
