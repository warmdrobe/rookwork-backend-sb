package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.projects.CreateProjectRequest;
import com.example.rookwork_backend_sb.dtos.projects.ProjectResponse;
import com.example.rookwork_backend_sb.dtos.projects.UpdateProjectRequest;
import com.example.rookwork_backend_sb.services.ProjectService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
/**
 * Controller exposing endpoints for project CRUD operations.
 */
@AllArgsConstructor
@RestController
@RequestMapping("api/projects")
public class ProjectController {
    private final ProjectService service;

    /**
     * Creates a new project and registers the current user as owner.
     *
     * @param request the project creation details payload
     * @return response entity containing the created ProjectResponse DTO
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(service.createProject(request));
    }

    /**
     * Retrieves all projects that the current user belongs to.
     *
     * @return response entity containing a list of ProjectResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProject() {
        return ResponseEntity.ok(service.getAllProject());
    }

    /**
     * Updates an existing project's metadata.
     *
     * @param projectId the unique identifier of the project to update
     * @param request the fields to update payload
     * @return response entity containing the updated ProjectResponse DTO
     */
    @PreAuthorize("@projectSecurity.isOwner(#projectId)")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(service.updateProject(projectId, request));
    }

    /**
     * Deletes a project.
     *
     * @param projectId the unique identifier of the project to delete
     * @return empty response entity indicating successful deletion
     */
    @PreAuthorize("@projectSecurity.isOwner(#projectId)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        service.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }
}