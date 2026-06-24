package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.IssueType;
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
    public String issueName;
    public String description;
    public IssueType issueType;
    public PriorityType priority;
    public LocalDate deadline;
    public UUID assignedToId;
    public Status status;
    public UUID parentId;
}
