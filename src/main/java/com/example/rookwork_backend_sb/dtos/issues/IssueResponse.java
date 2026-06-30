package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.projectstatus.ProjectStatusResponse;
import com.example.rookwork_backend_sb.entities.IssueType;
import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.dtos.subtasks.SubTaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {
    public UUID id;
    public String issueName;
    public String description;
    public IssueTypeResponse issueType;
    public PriorityType priority;
    public ProjectStatusResponse status;
    public UUID parentId;
    public UUID projectId;
    public List<UserSummary> assignees;
    public Instant deadline;
    public Instant createdAt;
    public Instant updatedAt;
    public List<AttachmentResponse> attachments;
    public List<SubTaskResponse> subtasks;
}
