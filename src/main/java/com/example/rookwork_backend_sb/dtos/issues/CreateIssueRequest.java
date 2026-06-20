package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.IssueType;
import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateIssueRequest {
    public String issueName;
    public IssueType issueType;
    public PriorityType priority;
    public String description;
    public Instant deadline;
    public Status status;
}
