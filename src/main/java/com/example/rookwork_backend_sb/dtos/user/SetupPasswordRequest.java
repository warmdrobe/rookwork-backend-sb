package com.example.rookwork_backend_sb.dtos.user;

import lombok.Data;

@Data
public class SetupPasswordRequest {
    private String otp;
    private String newPassword;
}
