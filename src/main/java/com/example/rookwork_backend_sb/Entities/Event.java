package com.example.rookwork_backend_sb.Entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="events")
public class Event {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name="event_description")
    private String eventDescription;

    @Column(name="deadline")
    private Date deadline;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;
}
