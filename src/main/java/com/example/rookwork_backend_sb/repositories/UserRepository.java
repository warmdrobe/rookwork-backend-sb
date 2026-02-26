package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    //tìm theo refresh token
    Optional<User> findByRefreshTokenHash(String refreshTokenHash);
}
