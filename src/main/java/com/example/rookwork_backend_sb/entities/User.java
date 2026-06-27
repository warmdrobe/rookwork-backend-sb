package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name="users")
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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
    private Instant refreshTokenExpiresAt;

    @Column(name="created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    @Column(name="deleted_at")
    private Instant deletedAt;

    @Column(name="job_title")
    private String jobTitle;

    @Column(name="organization")
    private String organization;

    @Column(name="location")
    private String location;

    @Column(name="email_public")
    @Builder.Default
    private boolean emailPublic = false;

    @Column(name="job_title_public")
    @Builder.Default
    private boolean jobTitlePublic = true;

    @Column(name="organization_public")
    @Builder.Default
    private boolean organizationPublic = true;

    @Column(name="location_public")
    @Builder.Default
    private boolean locationPublic = true;

    @Column(name="notify_issue_assigned")
    @Builder.Default
    private boolean notifyIssueAssigned = true;

    @Column(name="notify_mentioned")
    @Builder.Default
    private boolean notifyMentioned = true;

    @Column(name="notify_project_updates")
    @Builder.Default
    private boolean notifyProjectUpdates = false;

    @Column(name="notify_daily_digest")
    @Builder.Default
    private boolean notifyDailyDigest = false;

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