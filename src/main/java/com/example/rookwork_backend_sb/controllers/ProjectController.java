package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.projects.CreateProjectRequest;
import com.example.rookwork_backend_sb.dtos.projects.ProjectResponse;
import com.example.rookwork_backend_sb.services.ProjectService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api/projects")
public class ProjectController {
    private final ProjectService service;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request){
        return ResponseEntity.ok(service.createProject(request));
    }
    @GetMapping("/{userId}")
    public ResponseEntity<List<ProjectResponse>> getAllProject(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getAllProject(userId));
    }
}
