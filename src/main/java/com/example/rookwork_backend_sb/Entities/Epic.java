package com.example.rookwork_backend_sb.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="epics")
public class Epic {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="epic_name", nullable = false)
    private String epicName;

    @Column(name="epic_description")
    private String epicDescription;

    @Column(name="status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    //One epic belongs to a project only
    @ManyToOne
    @JoinColumn(name="project_id", nullable = false)
    private Project project;

    //One epic has stories
    @OneToMany(mappedBy = "epic")
    private Set<Story> stories = new HashSet<>();

    //One epic has tasks
    @OneToMany(mappedBy= "epic")
    private Set<Task> tasks = new HashSet<>();

}
