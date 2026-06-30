package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findById (UUID id);
    Optional<User> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActiveAt >= :since")
    long countActiveUsersSince(@Param("since") Instant since);
}
