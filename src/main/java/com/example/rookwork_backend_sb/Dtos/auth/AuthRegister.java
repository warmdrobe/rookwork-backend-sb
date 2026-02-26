package com.example.rookwork_backend_sb.Dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthRegister {
    public String email;
    public String profileName;
    public String password;
}
