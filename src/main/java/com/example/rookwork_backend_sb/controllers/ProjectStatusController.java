package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.projectstatus.*;
import com.example.rookwork_backend_sb.services.ProjectStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for managing the workflow status columns of a project.
 *
 * <p>All write operations are guarded by OWNER-only checks inside {@link ProjectStatusService}.
 *
 * <p>Base path: {@code /api/projects/{projectId}/statuses}
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("api/projects/{projectId}/statuses")
public class ProjectStatusController {

    private final ProjectStatusService projectStatusService;

    /**
     * Returns all status columns for the project, ordered by position.
     * Accessible by any project member.
     */
    @GetMapping
    public ResponseEntity<List<ProjectStatusResponse>> listStatuses(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectStatusService.listStatuses(projectId));
    }

    /**
     * Adds a new status column to the project's Kanban board.
     * Requires OWNER role.
     */
    @PostMapping
    public ResponseEntity<ProjectStatusResponse> createStatus(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectStatusService.createStatus(projectId, request));
    }

    /**
     * Updates the display name or color of an existing status column.
     * Requires OWNER role.
     */
    @PutMapping("/{statusId}")
    public ResponseEntity<ProjectStatusResponse> updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID statusId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(projectStatusService.updateStatus(projectId, statusId, request));
    }

    /**
     * Batch-reorders all status columns of a project (drag-and-drop).
     * Returns the full updated list ordered by new positions.
     * Requires OWNER role. Returns HTTP 409 if a concurrent modification is detected.
     */
    @PutMapping("/reorder")
    public ResponseEntity<List<ProjectStatusResponse>> reorderStatuses(
            @PathVariable UUID projectId,
            @Valid @RequestBody ReorderStatusRequest request) {
        return ResponseEntity.ok(projectStatusService.reorderStatuses(projectId, request));
    }

    /**
     * Deletes a status column after migrating its issues to a fallback status.
     * Requires OWNER role.
     */
    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID statusId,
            @Valid @RequestBody DeleteStatusRequest request) {
        projectStatusService.deleteStatus(projectId, statusId, request);
        return ResponseEntity.noContent().build();
    }
}
