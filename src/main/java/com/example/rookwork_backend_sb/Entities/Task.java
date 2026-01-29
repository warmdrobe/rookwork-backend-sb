package com.example.rookwork_backend_sb.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="tasks")
public class Task {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="task_title")
    private String taskTitle;

    @Column(name="task_description")
    private String taskDescription;

    @Column(name="deadline")
    private  Date deadline;

    @Column(name="status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    //One task belongs to a project
    @ManyToOne
    @JoinColumn(name="project_id", nullable = false)
    private Project project;

    //One task can also belong to an epic
    @ManyToOne
    @JoinColumn(name="epic_id")
    private Epic epic;

    //One task can also belong to a story
    @ManyToOne
    @JoinColumn(name="story_id")
    private Story story;

    //One task can have multiple subtasks
    @OneToMany(mappedBy = "task")
    private Set<SubTask> subtasks = new HashSet<>();
}
