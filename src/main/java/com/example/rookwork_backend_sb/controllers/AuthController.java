package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.auth.AuthRegister;
import com.example.rookwork_backend_sb.dtos.auth.AuthResponse;
import com.example.rookwork_backend_sb.dtos.auth.LoginRequest;
import com.example.rookwork_backend_sb.dtos.auth.GoogleLoginRequest;
import com.example.rookwork_backend_sb.dtos.auth.RefreshRequest;
import com.example.rookwork_backend_sb.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for user registration, authentication (login), and token refreshing.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {

  private final AuthService authService;

  /**
   * Authenticates user credentials and issues tokens.
   *
   * @param request the login request payload containing email and password
   * @return response entity containing access and refresh tokens
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  /**
   * Authenticates a Google ID token and issues tokens.
   *
   * @param request the Google login request containing the ID token
   * @return response entity containing access and refresh tokens
   */
  @PostMapping("/google")
  public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
    return ResponseEntity.ok(authService.googleLogin(request));
  }

  /**
   * Registers a new user account and issues tokens.
   *
   * @param authRegister the registration details payload
   * @return response entity containing access and refresh tokens
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody AuthRegister authRegister) {
    return ResponseEntity.ok(authService.register(authRegister));
  }

  /**
   * Reissues access and refresh tokens using a valid refresh token.
   *
   * @param dto the refresh token request payload
   * @return response entity containing new tokens or error message if unauthorized
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody RefreshRequest dto) {
    try {
      return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }
}
