package com.example.rookwork_backend_sb.dtos.issues;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import com.example.rookwork_backend_sb.entities.PriorityType;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateIssueRequest {
    @NotBlank(message = "Issue name must not be blank")
    @Size(max = 200, message = "Issue name must not exceed 200 characters")
    public String issueName;
    public UUID issueTypeId;
    public PriorityType priority;
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    public String description;
    public Instant deadline;
    /** UUID of the ProjectStatus column this issue starts in. */
    public UUID statusId;
}
