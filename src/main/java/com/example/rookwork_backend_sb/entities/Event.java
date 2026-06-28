package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;
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
    private Instant deadline;

    @Column(name="location")
    private String location;

    @Column(name="color")
    private String color;

    @Column(name="start_time")
    private Instant startTime;

    @Column(name="end_time")
    private Instant endTime;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_guests",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> guests;
}
