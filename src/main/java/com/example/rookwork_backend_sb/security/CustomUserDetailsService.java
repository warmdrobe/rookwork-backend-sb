package com.example.rookwork_backend_sb.security;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.rookwork_backend_sb.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        com.example.rookwork_backend_sb.entities.User user;

        // Nếu là email (login)
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found"));
        }
        // Nếu là UUID (JWT)
        else {
            user = userRepository.findById(UUID.fromString(identifier))
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found"));
        }

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is locked");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "NO_PASSWORD_SET")
                .roles(user.getSystemRole().name(), "USER")
                .disabled(!user.isActive())
                .build();
    }
}