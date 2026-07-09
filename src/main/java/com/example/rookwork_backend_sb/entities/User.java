package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
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

    @Column(name="email", nullable = false, unique = true)
    private String email;

    @Column(name="profile_name", length = 50)
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

    @Column(name="job_title", length = 100)
    private String jobTitle;

    @Column(name="organization", length = 100)
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(name="system_role", nullable = false)
    @Builder.Default
    private SystemRole systemRole = SystemRole.USER;

    @Column(name="location", length = 150)
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

    @Column(name="notify_comment")
    @Builder.Default
    private boolean notifyComment = true;

    @Column(name="notify_event_invited")
    @Builder.Default
    private boolean notifyEventInvited = true;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private java.time.Instant otpExpiry;

    @Column(name = "otp_failed_attempts", nullable = false)
    @Builder.Default
    private int otpFailedAttempts = 0;

    @Column(name = "last_password_change_at")
    private java.time.Instant lastPasswordChangeAt;

    @Column(name = "password_changes_this_month", nullable = false)
    @Builder.Default
    private int passwordChangesThisMonth = 0;

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