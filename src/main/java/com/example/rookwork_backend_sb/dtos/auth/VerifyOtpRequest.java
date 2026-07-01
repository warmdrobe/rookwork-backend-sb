package com.example.rookwork_backend_sb.dtos.auth;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String invitationId;
}
