package com.example.rookwork_backend_sb.dtos.issues;

import com.example.rookwork_backend_sb.entities.PriorityType;
import com.example.rookwork_backend_sb.entities.Status;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
public class UpdateIssueRequest {
    private String issueName;
    private String description;
    private PriorityType priority;
    private LocalDate deadline;
    private UUID assignedToId;
    private Status status;
    private UUID parentId;

    private final Set<String> dirtyFields = new HashSet<>();

    public void setIssueName(String issueName) {
        this.issueName = issueName;
        this.dirtyFields.add("issueName");
    }

    public void setDescription(String description) {
        this.description = description;
        this.dirtyFields.add("description");
    }

    public void setPriority(PriorityType priority) {
        this.priority = priority;
        this.dirtyFields.add("priority");
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
        this.dirtyFields.add("deadline");
    }

    public void setAssignedToId(UUID assignedToId) {
        this.assignedToId = assignedToId;
        this.dirtyFields.add("assignedToId");
    }

    public void setStatus(Status status) {
        this.status = status;
        this.dirtyFields.add("status");
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.dirtyFields.add("parentId");
    }

    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}
