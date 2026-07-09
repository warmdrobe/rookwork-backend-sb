package com.example.rookwork_backend_sb.dtos.projectstatus;

import com.example.rookwork_backend_sb.entities.StatusCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStatusRequest {

    @NotBlank(message = "Status name must not be blank")
    @Size(max = 100, message = "Status name must not exceed 100 characters")
    private String statusName;

    /** Optional hex/CSS color, e.g. "#3b82f6". Defaults to grey if omitted. */
    private String color;

    /**
     * Required: indicates how this status counts in progress reports.
     * Must be one of TO_DO, IN_PROGRESS, or DONE.
     */
    @NotNull(message = "statusCategory is required")
    private StatusCategory statusCategory;
}
