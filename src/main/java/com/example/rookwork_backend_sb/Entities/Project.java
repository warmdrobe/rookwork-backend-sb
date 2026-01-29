package com.example.rookwork_backend_sb.Entities;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;



@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="projects")
public class Project {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="access_modifier")
    private String accessModifier;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="project_name", nullable = false)
    private  String projectName;

    //Project <-> ProjectMember <-> User
    @OneToMany(mappedBy = "project")
    private Set<ProjectMember> projectMembers = new HashSet<>();

    //A Project has many Epics
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Epic> epics = new HashSet<>();

    //A Project has many stories
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Story> stories = new HashSet<>();

    //A Project has many tasks
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "project")
    private Set<Event> events = new HashSet<>();


}
