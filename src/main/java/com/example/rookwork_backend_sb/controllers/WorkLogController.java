package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.worklog.*;
import com.example.rookwork_backend_sb.services.WorkLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    // POST /api/work-logs
    @PostMapping
    public ResponseEntity<List<WorkLogResponse>> logWork(@RequestBody LogWorkRequest request) {
        return ResponseEntity.ok(workLogService.logWork(request));
    }

    // GET /api/work-logs/issue/:issueId
    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<WorkLogResponse>> getByIssue(@PathVariable UUID issueId) {
        return ResponseEntity.ok(workLogService.getByIssue(issueId));
    }

    // GET /api/work-logs/stats?period=weekly|monthly
    @GetMapping("/stats")
    public ResponseEntity<WorkStatsResponse> getStats(
            @RequestParam(defaultValue = "weekly") String period) {
        WorkStatsResponse stats = period.equals("monthly")
                ? workLogService.getMonthlyStats()
                : workLogService.getWeeklyStats();
        return ResponseEntity.ok(stats);
    }
}