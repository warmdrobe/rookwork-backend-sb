package com.example.rookwork_backend_sb.entities;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ActivityAction actionType;
    // 'created', 'completed', 'moved', 'commented',
    // 'uploaded', 'assigned', 'updated', 'deleted'

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private ActivityEntityType entityType;
    // 'issue', 'comment', 'file', 'subtask', 'member'

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // lưu JSON string, parse ở service layer

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
