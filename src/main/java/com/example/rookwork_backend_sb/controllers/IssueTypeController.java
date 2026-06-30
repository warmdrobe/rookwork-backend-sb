package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.issues.CreateIssueTypeRequest;
import com.example.rookwork_backend_sb.dtos.issues.IssueIconOption;
import com.example.rookwork_backend_sb.dtos.issues.IssueTypeResponse;
import com.example.rookwork_backend_sb.services.IssueTypeService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class IssueTypeController {
    private final IssueTypeService issueTypeService;

    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    @GetMapping("api/projects/{projectId}/issue-types")
    public ResponseEntity<List<IssueTypeResponse>> getIssueTypes(@PathVariable UUID projectId) {
        return ResponseEntity.ok(issueTypeService.getIssueTypes(projectId));
    }

    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    @PostMapping("api/projects/{projectId}/issue-types")
    public ResponseEntity<IssueTypeResponse> createIssueType(
            @PathVariable UUID projectId,
            @RequestBody CreateIssueTypeRequest request) {
        return ResponseEntity.ok(issueTypeService.createIssueType(projectId, request));
    }

    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    @DeleteMapping("api/projects/{projectId}/issue-types/{issueTypeId}")
    public ResponseEntity<Void> deleteIssueType(
            @PathVariable UUID projectId,
            @PathVariable UUID issueTypeId) {
        issueTypeService.deleteIssueType(projectId, issueTypeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("api/issue-types/icons")
    public ResponseEntity<List<IssueIconOption>> getSupportedIcons() {
        return ResponseEntity.ok(issueTypeService.getSupportedIcons());
    }
}
