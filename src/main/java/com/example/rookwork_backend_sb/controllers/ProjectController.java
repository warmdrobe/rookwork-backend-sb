package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.Dtos.projects.CreateProjectRequest;
import com.example.rookwork_backend_sb.Dtos.projects.ProjectResponse;
import com.example.rookwork_backend_sb.services.ProjectService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("api/projects")
public class ProjectController {
    private final ProjectService service;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request){
        return ResponseEntity.ok(service.createProject(request));
    }
}
