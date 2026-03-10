package com.example.rookwork_backend_sb.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    public String accessToken;
    public String refreshToken;
}
