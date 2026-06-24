package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateIssueRequest {
    private String issueName;
    private String description;
    private PriorityType priority;
    private LocalDate deadline;
    private UUID assignedToId;
    private Status status;
    private UUID parentId;
}
