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
@Table(name="stories")
public class Story {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="story_title", nullable = false)//Not NULL
    private String storyTitle;

    @Column(name="story_description")
    private String storyDescription;

    @Column(name="priority")
    private  String priotity;

    @Column(name="status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    //A story belongs to a project
    @ManyToOne
    @JoinColumn(name="project_id", nullable = false)
    private Project project;

    //A story can also belong to an epic
    @ManyToOne
    @JoinColumn(name="epic_id")
    private Epic epic;

    //A story can also have many tasks
    @OneToMany(mappedBy = "story")
    private Set<Task> tasks = new HashSet<>();

}
