package com.example.rookwork_backend_sb.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="accounts")
public class Account {

    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="username", nullable = false)
    private String username;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="is_active")
    private boolean isActive;

    @Column(name="is_verified")
    private boolean isVerified;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "account")
    private User user;
}
