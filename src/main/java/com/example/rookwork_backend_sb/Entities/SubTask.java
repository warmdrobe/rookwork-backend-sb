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
@Table(name="subtasks")
public class SubTask {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="subtask_name")
    private String subtaskName;

    @Column(name="subtask_description")
    private String subtaskDescription;

    @Column(name="is_done")
    private boolean isDone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    //A Subtask belongs to a task
    @ManyToOne
    @JoinColumn(name="task_id", nullable = false)
    private Task task;

}
