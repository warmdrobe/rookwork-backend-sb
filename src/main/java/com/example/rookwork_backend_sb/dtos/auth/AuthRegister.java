package com.example.rookwork_backend_sb.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthRegister {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    public String email;

    @NotBlank(message = "Display name must not be blank")
    @Size(max = 50, message = "Display name must not exceed 50 characters")
    public String profileName;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    public String password;

    public UUID invitationId;
}
