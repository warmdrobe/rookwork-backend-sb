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

@AllArgsConstructor
@RestController
@RequestMapping
public class IssueController {
    private final IssueService issueService;

    @PostMapping("api/projects/{projectId}/issues")
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable UUID projectId,
            @RequestBody CreateIssueRequest request) {
        return ResponseEntity.ok(issueService.createIssue(projectId, request));
    }

    @PutMapping("api/projects/{projectId}/issues/{issueId}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody UpdateIssueRequest request) {
        return ResponseEntity.ok(issueService.updateIssue(projectId, issueId, request));
    }

    @DeleteMapping("api/projects/{projectId}/issues/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId) {
        issueService.deleteIssue(projectId, issueId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("api/projects/{projectId}/issues")
    public ResponseEntity<List<IssueResponse>> getAllIssues(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(issueService.getAllIssue(projectId));
    }

    @GetMapping("api/issues/assigned")
    public ResponseEntity<List<IssueResponse>> getAssignedIssues() {
        return ResponseEntity.ok(issueService.getAllByAssignedToId());
    }

    @GetMapping("api/issues/{issueId}")
    public ResponseEntity<IssueResponse> getIssueById(
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(issueService.getIssueById(issueId));
    }
}
