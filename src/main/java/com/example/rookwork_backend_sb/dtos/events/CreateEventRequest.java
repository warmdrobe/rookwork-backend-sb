package com.example.rookwork_backend_sb.dtos.events;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventRequest {
    private String eventName;
    private String eventDescription;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private String color;
    private List<String> guestEmails;
    private UUID projectId;
}
