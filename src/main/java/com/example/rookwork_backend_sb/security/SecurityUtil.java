package com.example.rookwork_backend_sb.security;

import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;
    
    public UUID getCurrentUserId()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated");
        }

        return UUID.fromString(authentication.getName());
    }

    public boolean isCurrentUserAdmin() {
        try {
            UUID userId = getCurrentUserId();
            return userRepository.findById(userId)
                    .map(u -> u.isAdmin())
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
