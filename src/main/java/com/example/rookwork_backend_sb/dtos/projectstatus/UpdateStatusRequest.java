package com.example.rookwork_backend_sb.dtos.projectstatus;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @Size(max = 100, message = "Status name must not exceed 100 characters")
    private String statusName;

    /** Optional hex/CSS color string, e.g. "#10b981". */
    private String color;
}
