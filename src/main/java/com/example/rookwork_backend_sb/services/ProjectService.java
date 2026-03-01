package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.Dtos.projects.CreateProjectRequest;
import com.example.rookwork_backend_sb.Dtos.projects.ProjectResponse;
import com.example.rookwork_backend_sb.Dtos.projects.UpdateProjectRequest;
import com.example.rookwork_backend_sb.Entities.*;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SecurityUtil securityUtil;

    //Create project
    public ProjectResponse createProject(CreateProjectRequest request){
        UUID currentUserId = securityUtil.getCurrentUserId();
        if(currentUserId == null)
            throw new RuntimeException("Authenticated");

        User user = userRepository.findById(currentUserId)
                .orElseThrow(()->new RuntimeException("User not found"));
        Project project = Project.builder()
                .projectName(request.projectName)
                .isPrivate(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        projectRepository.save(project);

        ProjectMember projectMember = ProjectMember.builder()
                .id(new ProjectMemberId(currentUserId,project.getId()))
                .user(user)
                .project(project)
                .role(ProjectRole.OWNER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        projectMemberRepository.save(projectMember);

        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setPrivate(project.isPrivate());
        response.setOwnerName(user.getProfileName());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        return response;
    }

    // Update project
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new RuntimeException("Not a member of this project"));

        if (member.getRole() != ProjectRole.OWNER) {
            throw new RuntimeException("Only OWNER can update project");
        }

        Project project = member.getProject();

        if (request.getProjectName() != null)
            project.setProjectName(request.getProjectName());

        if (request.getIsPrivate() != null)
            project.setPrivate(request.getIsPrivate());

        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setPrivate(project.isPrivate());
        response.setOwnerName(member.getUser().getProfileName());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        return response;
    }

    //delete project
    public void deleteProject(UUID projectId){
        UUID currentUserId = securityUtil.getCurrentUserId();
        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new RuntimeException("Not a member of project"));

        if(member.getRole() != ProjectRole.OWNER){
            throw new RuntimeException("Only on can delete this project");
        }

        projectRepository.deleteById(projectId);
    }

    //invite into project
}
