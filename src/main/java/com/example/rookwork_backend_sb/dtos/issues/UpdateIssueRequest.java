package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.IssueType;
import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateIssueRequest {
    public String issueName;
    public String description;
    public IssueType issueType;
    public PriorityType priority;
    public LocalDate deadline;
    /** null = không thay đổi, [] = xóa hết assignee, [id1,id2] = set mới */
    public List<UUID> assigneeIds;
    public Status status;
    public UUID parentId;
}
