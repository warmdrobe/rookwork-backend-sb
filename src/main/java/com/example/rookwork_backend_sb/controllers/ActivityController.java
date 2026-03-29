package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.activities.ActivityResponse;
import com.example.rookwork_backend_sb.entities.Activity;
import com.example.rookwork_backend_sb.services.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityResponse>> getProjectActivities(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(activityService.getProjectActivityResponses(projectId, limit));
    }

    @GetMapping("/issues/{issueId}/activities")
    public ResponseEntity<List<ActivityResponse>> getIssueActivities(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(activityService.getIssueActivity(projectId, issueId, limit));
    }
}