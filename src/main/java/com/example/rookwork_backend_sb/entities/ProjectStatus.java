package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a workflow column (status) within a specific project.
 *
 * <p>Each project owns its own list of statuses (e.g. "Backlog", "In QA", "Deployed").
 * A {@link StatusCategory} is stored alongside the display name so that progress
 * reports and dashboards can aggregate across projects with different custom names.
 *
 * <p>The {@code version} field enables optimistic locking to prevent concurrent
 * drag-and-drop reorder operations from silently overwriting each other.
 */
@Entity
@Table(
    name = "project_statuses",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_project_position",
        columnNames = {"project_id", "position"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStatus {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status_name", nullable = false, length = 100)
    private String statusName;

    /** Hex color code or any CSS-compatible color string, e.g. "#3b82f6". */
    @Column(name = "color", length = 50)
    private String color;

    /** 1-based ordering of this status column on the Kanban board. */
    @Column(name = "position", nullable = false)
    private int position;

    /**
     * Semantic category used for reporting and completion metrics.
     * Must not be null — every custom status must map to a known category.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_category", nullable = false, length = 20)
    private StatusCategory statusCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Optimistic-lock version field. Prevents concurrent reorder conflicts. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
