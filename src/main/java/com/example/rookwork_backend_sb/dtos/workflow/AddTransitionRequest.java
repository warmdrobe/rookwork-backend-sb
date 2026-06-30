package com.example.rookwork_backend_sb.dtos.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTransitionRequest {
    @NotNull(message = "fromStatusId is required")
    private UUID fromStatusId;

    @NotNull(message = "toStatusId is required")
    private UUID toStatusId;
}
