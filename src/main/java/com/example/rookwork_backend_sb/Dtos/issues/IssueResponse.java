package com.example.rookwork_backend_sb.Dtos.issues;

import com.example.rookwork_backend_sb.Entities.IssueType;
import com.example.rookwork_backend_sb.Entities.PriorityType;
import com.example.rookwork_backend_sb.Entities.Status;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Priority;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class IssueResponse {
    public UUID id;
    public String issueName;
    public String description;
    public IssueType issueType;
    public PriorityType priority;
    public Status status;
    public UUID parentId;
    public UUID projectId;
    public LocalDateTime deadline;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

}
