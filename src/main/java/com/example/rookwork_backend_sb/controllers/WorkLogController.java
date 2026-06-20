package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.worklog.*;
import com.example.rookwork_backend_sb.services.WorkLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for logging work hours on issues and retrieving productivity stats.
 */
@RestController
@RequestMapping("api/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    /**
     * Logs work hours.
     *
     * @param request the log work details containing issue ID and start/end times
     * @return response entity containing a list of split WorkLogResponse DTOs
     */
    @PostMapping
    public ResponseEntity<List<WorkLogResponse>> logWork(@RequestBody LogWorkRequest request) {
        return ResponseEntity.ok(workLogService.logWork(request));
    }

    /**
     * Retrieves all work logs logged against a specific issue.
     *
     * @param issueId the unique identifier of the issue
     * @return response entity containing a list of WorkLogResponse DTOs
     */
    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<WorkLogResponse>> getByIssue(@PathVariable UUID issueId) {
        return ResponseEntity.ok(workLogService.getByIssue(issueId));
    }

    /**
     * Retrieves work aggregation statistics for either a weekly or monthly period.
     *
     * @param period the stats period ("weekly" or "monthly", default is "weekly")
     * @return response entity containing the aggregated WorkStatsResponse DTO
     */
    @GetMapping("/stats")
    public ResponseEntity<WorkStatsResponse> getStats(
            @RequestParam(defaultValue = "weekly") String period) {
        WorkStatsResponse stats = period.equals("monthly")
                ? workLogService.getMonthlyStats()
                : workLogService.getWeeklyStats();
        return ResponseEntity.ok(stats);
    }
}