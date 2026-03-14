package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.entities.IssueType;
import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {
    public UUID id;
    public String issueName;
    public String description;
    public IssueType issueType;
    public PriorityType priority;
    public Status status;
    public UUID parentId;
    public UUID projectId;
    public UserSummary assignedTo;
    public LocalDateTime deadline;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
