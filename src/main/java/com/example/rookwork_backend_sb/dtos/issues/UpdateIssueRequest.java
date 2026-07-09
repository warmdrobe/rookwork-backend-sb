package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.PriorityType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateIssueRequest {
    @Size(max = 200, message = "Issue name must not exceed 200 characters")
    public String issueName;
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    public String description;
    public UUID issueTypeId;
    public PriorityType priority;
    public Instant startDate;
    public Instant deadline;
    /** null = không thay đổi, [] = xóa hết assignee, [id1,id2] = set mới */
    public List<UUID> assigneeIds;
    public List<UUID> dependencyIds;
    /** null = no change. UUID of the target ProjectStatus column. */
    public UUID statusId;
    public UUID parentId;
}
