package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.workflow.BulkWorkflowRequest;
import com.example.rookwork_backend_sb.dtos.workflow.WorkflowResponse;
import com.example.rookwork_backend_sb.services.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/projects/{projectId}/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable UUID projectId) {
        return ResponseEntity.ok(workflowService.getWorkflow(projectId));
    }

    @PutMapping
    public ResponseEntity<WorkflowResponse> replaceWorkflow(
            @PathVariable UUID projectId,
            @Valid @RequestBody BulkWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.replaceWorkflow(projectId, request));
    }
}
