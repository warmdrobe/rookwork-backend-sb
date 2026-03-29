package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.projects.CreateProjectRequest;
import com.example.rookwork_backend_sb.dtos.projects.ProjectResponse;
import com.example.rookwork_backend_sb.dtos.projects.UpdateProjectRequest;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SecurityUtil securityUtil;
    private final IssueRepository issueRepository;
    /// Create project
    public ProjectResponse createProject(CreateProjectRequest request){
        UUID currentUserId = securityUtil.getCurrentUserId();
        if(currentUserId == null)
            throw new UnauthorizedException("Not authenticated");

        User user = userRepository.findById(currentUserId)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        Project project = Project.builder()
                .projectName(request.projectName)
                .description(request.description)
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

    /// Update project
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Not a member of this project"));

        if (member.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only OWNER can update project");
        }

        Project project = member.getProject();

        if (request.getProjectName() != null)
            project.setProjectName(request.getProjectName());

        if (request.getDescription() != null)
            project.setDescription(request.getDescription());

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

    /// Delete project
    public void deleteProject(UUID projectId){
        UUID currentUserId = securityUtil.getCurrentUserId();
        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Not a member of project"));

        if(member.getRole() != ProjectRole.OWNER){
            throw new ForbiddenException("Only OWNER can delete this project");
        }

        projectRepository.deleteById(projectId);
    }

    /// Get all project
    public List<ProjectResponse> getAllProject() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null)
            throw new UnauthorizedException("Not authenticated");

        return projectMemberRepository.findAllByUser_Id(currentUserId)
                .stream()
                .map(member -> {
                    // Lấy tất cả members của project đó
                    List<UserSummary> members = projectMemberRepository
                            .findAllByProject_Id(member.getProject().getId())
                            .stream()
                            .map(pm -> UserSummary.builder()
                                    .id(pm.getUser().getId())
                                    .profileName(pm.getUser().getProfileName())
                                    .picture(pm.getUser().getPicture())
                                    .build())
                            .collect(Collectors.toList());

                    long total = issueRepository.countByProjectId(member.getProject().getId());
                    long done = issueRepository.countByProjectIdAndStatus(member.getProject().getId(), Status.DONE);

                    return ProjectResponse.builder()
                            .id(member.getProject().getId())
                            .projectName(member.getProject().getProjectName())
                            .description(member.getProject().getDescription())
                            .isPrivate(member.getProject().isPrivate())
                            .ownerName(member.getUser().getProfileName())
                            .members(members)
                            .totalIssues(total)
                            .doneIssues(done)
                            .createdAt(member.getProject().getCreatedAt())
                            .updatedAt(member.getProject().getUpdatedAt())
                            .build();
                })

                .collect(Collectors.toList());
    }
}
