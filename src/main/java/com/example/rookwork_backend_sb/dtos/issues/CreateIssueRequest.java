package com.example.rookwork_backend_sb.dtos.issues;

import java.util.UUID;
import com.example.rookwork_backend_sb.entities.PriorityType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateIssueRequest {
    public String issueName;
    public UUID issueTypeId;
    public PriorityType priority;
    public String description;
    public Instant deadline;
    /** UUID of the ProjectStatus column this issue starts in. */
    public UUID statusId;
}
