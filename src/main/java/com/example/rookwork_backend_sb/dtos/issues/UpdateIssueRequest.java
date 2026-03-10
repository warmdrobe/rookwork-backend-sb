package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateIssueRequest {
    public String issueName;
    public String description;
    public PriorityType priority;
    public LocalDate deadline;
    public UUID assignedToId;
    public Status status;
    public UUID parentId;
}
