package com.example.rookwork_backend_sb.dtos.projectstatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeleteStatusRequest {

    /**
     * The status that all issues currently assigned to the deleted status
     * will be moved to before deletion.
     * Must belong to the same project.
     */
    @NotNull(message = "fallbackStatusId is required")
    private UUID fallbackStatusId;
}
