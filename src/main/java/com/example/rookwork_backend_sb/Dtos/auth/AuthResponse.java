package com.example.rookwork_backend_sb.Dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    public String accessToken;
    public String refreshToken;
}
