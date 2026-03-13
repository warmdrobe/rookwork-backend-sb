package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findById (UUID id);
    Optional<User> findByRefreshTokenHash(String refreshTokenHash);
}
