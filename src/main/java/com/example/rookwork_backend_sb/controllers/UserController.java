package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
  private final SecurityUtil securityUtil;
  private final UserRepository userRepository;

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
        .build());
  }
}
