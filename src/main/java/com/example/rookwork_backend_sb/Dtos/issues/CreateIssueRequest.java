package com.example.rookwork_backend_sb.Dtos.issues;

import com.example.rookwork_backend_sb.Entities.IssueType;
import com.example.rookwork_backend_sb.Entities.PriorityType;
import com.example.rookwork_backend_sb.Entities.Project;
import com.example.rookwork_backend_sb.Entities.Status;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateIssueRequest {
    public String issueName;
    public IssueType issueType;
    public PriorityType priority;
    public String description;
    public LocalDateTime deadline;
    public Status status;
}
