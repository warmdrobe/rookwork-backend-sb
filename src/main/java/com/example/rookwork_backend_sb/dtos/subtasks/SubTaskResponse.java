package com.example.rookwork_backend_sb.dtos.subtasks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskResponse {
    private UUID id;
    private String subtaskName;
    private String subtaskDescription;
    private boolean isDone;
    private UUID issueId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}