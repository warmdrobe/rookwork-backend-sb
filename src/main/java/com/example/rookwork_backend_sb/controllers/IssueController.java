package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.issues.CreateIssueRequest;
import com.example.rookwork_backend_sb.dtos.issues.IssueResponse;
import com.example.rookwork_backend_sb.dtos.issues.UpdateIssueRequest;
import com.example.rookwork_backend_sb.services.IssueService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for issue CRUD operations, project issue listings, and user assignments.
 */
@AllArgsConstructor
@RestController
@RequestMapping
public class IssueController {
    private final IssueService issueService;

    /**
     * Creates a new issue within a project.
     *
     * @param projectId the unique identifier of the project
     * @param request the issue details payload
     * @return response entity containing the created IssueResponse DTO
     */
    @PostMapping("api/projects/{projectId}/issues")
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable UUID projectId,
            @RequestBody CreateIssueRequest request) {
        return ResponseEntity.ok(issueService.createIssue(projectId, request));
    }

    /**
     * Updates an existing issue's fields.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue to update
     * @param request the updated fields payload
     * @return response entity containing the updated IssueResponse DTO
     */
    @PutMapping("api/projects/{projectId}/issues/{issueId}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(issueService.updateIssue(projectId, issueId, request));
    }

    /**
     * Deletes an issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue to delete
     * @return empty response entity indicating successful deletion
     */
    @DeleteMapping("api/projects/{projectId}/issues/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId) {
        issueService.deleteIssue(projectId, issueId);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves all issues belonging to a project.
     *
     * @param projectId the unique identifier of the project
     * @return response entity containing a list of IssueResponse DTOs
     */
    @GetMapping("api/projects/{projectId}/issues")
    public ResponseEntity<List<IssueResponse>> getAllIssues(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(issueService.getAllIssue(projectId));
    }

    /**
     * Retrieves all issues assigned to the currently authenticated user.
     *
     * @return response entity containing a list of assigned IssueResponse DTOs
     */
    @GetMapping("api/issues/assigned")
    public ResponseEntity<List<IssueResponse>> getAssignedIssues() {
        return ResponseEntity.ok(issueService.getAllByAssignedToId());
    }

    /**
     * Retrieves a specific issue by its ID.
     *
     * @param issueId the unique identifier of the issue
     * @return response entity containing the IssueResponse DTO
     */
    @GetMapping("api/issues/{issueId}")
    public ResponseEntity<IssueResponse> getIssueById(
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(issueService.getIssueById(issueId));
    }
}
