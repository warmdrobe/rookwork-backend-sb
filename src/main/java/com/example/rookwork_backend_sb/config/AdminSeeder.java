package com.example.rookwork_backend_sb.config;

import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running AdminSeeder...");

        // 1. Revoke admin rights from all users except admin@rookwork.com
        List<User> allAdmins = userRepository.findAll().stream()
                .filter(User::isAdmin)
                .toList();

        for (User u : allAdmins) {
            if (!"admin@rookwork.com".equalsIgnoreCase(u.getEmail())) {
                u.setAdmin(false);
                userRepository.save(u);
                log.info("Revoked admin rights from: {}", u.getEmail());
            }
        }

        // 2. Seed default admin if it doesn't exist
        Optional<User> adminOpt = userRepository.findByEmail("admin@rookwork.com");
        if (adminOpt.isEmpty()) {
            User defaultAdmin = new User();
            defaultAdmin.setEmail("admin@rookwork.com");
            defaultAdmin.setProfileName("System Admin");
            defaultAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
            defaultAdmin.setActive(true);
            defaultAdmin.setVerified(true);
            defaultAdmin.setAdmin(true);
            userRepository.save(defaultAdmin);
            log.info("Seeded default admin account: admin@rookwork.com / admin123");
        } else {
            User existingAdmin = adminOpt.get();
            if (!existingAdmin.isAdmin()) {
                existingAdmin.setAdmin(true);
                userRepository.save(existingAdmin);
                log.info("Restored admin rights to default admin account.");
            }
        }
    }
}
