package com.example.rookwork_backend_sb.dtos.events;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {
    private UUID id;
    private String eventName;
    private String eventDescription;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private String color;
    private UserSummary creator;
    private List<UserSummary> guests;
    private UUID projectId;
    private Instant createdAt;
    private Instant updatedAt;
}
