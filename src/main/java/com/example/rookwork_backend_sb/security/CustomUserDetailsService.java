package com.example.rookwork_backend_sb.security;

import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}