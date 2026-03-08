package com.example.rookwork_backend_sb.Dtos.issues;

import com.example.rookwork_backend_sb.Entities.PriorityType;
import com.example.rookwork_backend_sb.Entities.Status;
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
