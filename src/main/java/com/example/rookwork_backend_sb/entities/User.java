package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name="users")
public class User {

    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="profile_name")
    private String profileName;

    @Column(name="picture")
    private String picture;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="is_active")
    private boolean isActive;

    @Column(name="is_verified")
    private boolean isVerified;

    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name="refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private Set<ProjectMember> projectMembers = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<File> files = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Event> events = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Notification> notifications = new HashSet<>();
}