package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.Dtos.issues.CreateIssueRequest;
import com.example.rookwork_backend_sb.Dtos.issues.IssueResponse;
import com.example.rookwork_backend_sb.Entities.Issue;
import com.example.rookwork_backend_sb.services.IssueService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api/issues")
public class IssueController {
    private final IssueService issueService;

    @PostMapping("/{projectId}")
    public ResponseEntity<IssueResponse> createIssue(@PathVariable UUID projectId, @RequestBody CreateIssueRequest request) {
        return ResponseEntity.ok(issueService.createIssue(projectId, request));
    }
}
