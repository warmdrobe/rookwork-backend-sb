package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="subtasks")
public class SubTask {

    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="subtask_name", length = 200)
    private String subtaskName;

    @Column(name="subtask_description")
    private String subtaskDescription;

    @Column(name="is_done")
    private boolean isDone;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;
}