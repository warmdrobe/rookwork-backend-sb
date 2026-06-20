package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.activities.ActivityResponse;
import com.example.rookwork_backend_sb.services.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for retrieving project and issue activity logs.
 */
@RestController
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
public class ActivityController {

  private final ActivityService activityService;

  /**
   * Retrieves a list of recent activities for a project.
   *
   * @param projectId the unique identifier of the project
   * @param limit the maximum number of activity logs to return (default is 20)
   * @return response entity containing a list of ActivityResponse DTOs
   */
  @GetMapping("/activities")
  public ResponseEntity<List<ActivityResponse>> getProjectActivities(
      @PathVariable UUID projectId,
      @RequestParam(defaultValue = "20") int limit) {

    return ResponseEntity.ok(activityService.getProjectActivityResponses(projectId, limit));
  }

  /**
   * Retrieves a list of recent activities specific to an issue.
   *
   * @param projectId the unique identifier of the project
   * @param issueId the unique identifier of the issue
   * @param limit the maximum number of activity logs to return (default is 20)
   * @return response entity containing a list of ActivityResponse DTOs
   */
  @GetMapping("/issues/{issueId}/activities")
  public ResponseEntity<List<ActivityResponse>> getIssueActivities(
      @PathVariable UUID projectId,
      @PathVariable UUID issueId,
      @RequestParam(defaultValue = "20") int limit) {

    return ResponseEntity.ok(activityService.getIssueActivity(projectId, issueId, limit));
  }
}
