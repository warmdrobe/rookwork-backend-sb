package com.example.rookwork_backend_sb.dtos.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    private UUID projectId;
    private List<TransitionDto> transitions;
    /** If true, no transitions exist yet, so any transition is allowed. */
    private boolean openWorkflow;
}
