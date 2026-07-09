package com.example.rookwork_backend_sb.dtos.events;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventRequest {
    private String eventName;
    private String eventDescription;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private String color;
    private List<String> guestEmails;
}
