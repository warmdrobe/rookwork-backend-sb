package com.example.rookwork_backend_sb.dtos.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkWorkflowRequest {
    @NotNull(message = "transitions list cannot be null")
    @Valid
    private List<AddTransitionRequest> transitions;
}
