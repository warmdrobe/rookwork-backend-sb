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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class handling project operations including creation, metadata updates, deletion, and retrieval.
 */
@AllArgsConstructor
@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SecurityUtil securityUtil;
    private final IssueRepository issueRepository;
    private final S3Service s3Service;

    /**
     * Creates a new project and sets the creator as the project owner.
     *
     * @param request the project creation details
     * @return the created ProjectResponse DTO
     * @throws UnauthorizedException if user is not authenticated
     * @throws ResourceNotFoundException if user is not found in database
     */
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
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        projectRepository.save(project);

        // Automatically assign the creator as the OWNER member of the new project
        ProjectMember projectMember = ProjectMember.builder()
                .id(new ProjectMemberId(currentUserId,project.getId()))
                .user(user)
                .project(project)
                .role(ProjectRole.OWNER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
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

    /**
     * Updates project metadata (name, description, privacy). Only allowed for project OWNERs.
     *
     * @param projectId the unique identifier of the project to update
     * @param request the fields to update
     * @return the updated ProjectResponse DTO
     * @throws ResourceNotFoundException if user is not a member of the project
     * @throws ForbiddenException if user is not the project owner
     */
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Not a member of this project"));

        // Only the OWNER of the project has permission to edit project settings
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

        project.setUpdatedAt(Instant.now());
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

    /**
     * Deletes a project. Only allowed for project OWNERs.
     *
     * @param projectId the unique identifier of the project to delete
     * @throws ResourceNotFoundException if user is not a member of the project
     * @throws ForbiddenException if user is not the project owner
     */
    public void deleteProject(UUID projectId){
        UUID currentUserId = securityUtil.getCurrentUserId();
        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Not a member of project"));

        // Only the OWNER can delete the project entirely
        if(member.getRole() != ProjectRole.OWNER){
            throw new ForbiddenException("Only OWNER can delete this project");
        }

        projectRepository.deleteById(projectId);
    }

    /**
     * Retrieves all projects that the current user is a member of, along with statistics.
     *
     * @return a list of ProjectResponse DTOs containing members list and issue statistics
     * @throws UnauthorizedException if user is not authenticated
     */
    public List<ProjectResponse> getAllProject() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null)
            throw new UnauthorizedException("Not authenticated");

        return projectMemberRepository.findAllByUser_Id(currentUserId)
                .stream()
                .map(member -> {
                    // Fetch all project members details
                    List<UserSummary> members = projectMemberRepository
                            .findAllByProject_Id(member.getProject().getId())
                            .stream()
                            .map(pm -> UserSummary.builder()
                                     .id(pm.getUser().getId())
                                     .profileName(pm.getUser().getProfileName())
                                     .picture(s3Service.getAvatarUrl(pm.getUser().getPicture()))
                                     .email(pm.getUser().getEmail())
                                     .role(pm.getRole() != null ? pm.getRole().name() : null)
                                     .build())
                            .collect(Collectors.toList());

                    // Count total and resolved issues within the project
                    long total = issueRepository.countByProjectId(member.getProject().getId());
                    long done = issueRepository.countByProjectIdAndStatus(member.getProject().getId(), Status.DONE);

                    String ownerName = members.stream()
                            .filter(m -> "OWNER".equals(m.getRole()))
                            .map(UserSummary::getProfileName)
                            .findFirst()
                            .orElse(member.getUser().getProfileName());

                    return ProjectResponse.builder()
                            .id(member.getProject().getId())
                            .projectName(member.getProject().getProjectName())
                            .description(member.getProject().getDescription())
                            .isPrivate(member.getProject().isPrivate())
                            .ownerName(ownerName)
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
