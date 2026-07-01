package com.example.rookwork_backend_sb.dtos.auth;

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
    public String email;
    public String profileName;
    public String password;
    public UUID invitationId;
}
