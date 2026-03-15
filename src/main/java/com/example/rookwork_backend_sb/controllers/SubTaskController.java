package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.subtasks.CreateSubTaskRequest;
import com.example.rookwork_backend_sb.dtos.subtasks.SubTaskResponse;
import com.example.rookwork_backend_sb.dtos.subtasks.UpdateSubTaskRequest;
import com.example.rookwork_backend_sb.services.SubTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/projects/{projectId}/issues/{issueId}/subtasks")
@RequiredArgsConstructor
public class SubTaskController {

    private final SubTaskService subTaskService;

    @PostMapping
    public ResponseEntity<SubTaskResponse> createSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody CreateSubTaskRequest request) {
        return ResponseEntity.ok(subTaskService.createSubTask(projectId, issueId, request));
    }

    @PutMapping("/{subtaskId}")
    public ResponseEntity<SubTaskResponse> updateSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID subtaskId,
            @RequestBody UpdateSubTaskRequest request) {
        return ResponseEntity.ok(subTaskService.updateSubTask(projectId, issueId, subtaskId, request));
    }

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubTask(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID subtaskId) {
        subTaskService.deleteSubTask(projectId, issueId, subtaskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SubTaskResponse>> getSubTasks(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(subTaskService.getSubTasks(projectId, issueId));
    }
}