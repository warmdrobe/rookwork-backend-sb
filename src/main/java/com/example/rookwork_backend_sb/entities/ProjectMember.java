package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="project_members")
public class ProjectMember {
    @EmbeddedId
    private ProjectMemberId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name="project_id", nullable = false)
    private Project project;

    @Column(name="role")
    @Enumerated(EnumType.STRING)
    private ProjectRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


}
