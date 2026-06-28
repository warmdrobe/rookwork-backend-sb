package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.subtasks.CreateSubTaskRequest;
import com.example.rookwork_backend_sb.dtos.subtasks.SubTaskResponse;
import com.example.rookwork_backend_sb.dtos.subtasks.UpdateSubTaskRequest;
import com.example.rookwork_backend_sb.services.SubTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for subtask CRUD operations under issues.
 */
@RestController
@RequestMapping("api/projects/{projectId}/issues/{issueId}/subtasks")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isMember(#projectId)")
public class SubTaskController {

    private final SubTaskService subTaskService;

    /**
     * Creates a new subtask under an issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param request the subtask details payload
     * @return response entity containing the created SubTaskResponse DTO
     */
    @PostMapping
    public ResponseEntity<SubTaskResponse> createSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody CreateSubTaskRequest request) {
        return ResponseEntity.ok(subTaskService.createSubTask(projectId, issueId, request));
    }

    /**
     * Updates an existing subtask.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param subtaskId the unique identifier of the subtask to update
     * @param request the updated fields payload
     * @return response entity containing the updated SubTaskResponse DTO
     */
    @PutMapping("/{subtaskId}")
    public ResponseEntity<SubTaskResponse> updateSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID subtaskId,
            @RequestBody UpdateSubTaskRequest request) {
        return ResponseEntity.ok(subTaskService.updateSubTask(projectId, issueId, subtaskId, request));
    }

    /**
     * Deletes a subtask.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param subtaskId the unique identifier of the subtask to delete
     * @return response entity with no content
     */
    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID subtaskId) {
        subTaskService.deleteSubTask(projectId, issueId, subtaskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all subtasks for a specific issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @return response entity containing a list of SubTaskResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<SubTaskResponse>> getSubTasks(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(subTaskService.getSubTasks(projectId, issueId));
    }
}