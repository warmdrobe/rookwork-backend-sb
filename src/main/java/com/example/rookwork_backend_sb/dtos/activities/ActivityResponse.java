package com.example.rookwork_backend_sb.dtos.activities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private UUID id;
    private String actorName;
    private String actorPicture;
    private String actionType;   // CREATED, MOVED, ASSIGNED, etc.
    private String entityType;   // ISSUE, COMMENT, etc.
    private UUID entityId;
    private String entityName;
    private String metadata;     // JSON string
    private Instant createdAt;
}