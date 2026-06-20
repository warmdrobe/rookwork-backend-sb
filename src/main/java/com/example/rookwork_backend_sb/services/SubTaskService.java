package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.subtasks.CreateSubTaskRequest;
import com.example.rookwork_backend_sb.dtos.subtasks.SubTaskResponse;
import com.example.rookwork_backend_sb.dtos.subtasks.UpdateSubTaskRequest;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service class handling operations on subtasks, including creation, status updates, deletion, and queries.
 */
@Service
@RequiredArgsConstructor
public class SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final SecurityUtil securityUtil;

    /**
     * Creates a new subtask under a specified issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param request the subtask details
     * @return the created SubTaskResponse DTO
     * @throws ForbiddenException if the user is not a project member
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ResourceNotFoundException if the project or issue is not found
     */
    @Transactional
    public SubTaskResponse createSubTask(UUID projectId, UUID issueId, CreateSubTaskRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Check project membership
        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        SubTask subTask = SubTask.builder()
                .subtaskName(request.getSubtaskName())
                .subtaskDescription(request.getSubtaskDescription())
                .isDone(false)
                .issue(issue)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        subTaskRepository.save(subTask);

        // Log subtask creation activity
        activityService.log(
                project,
                currentUser,
                ActivityAction.CREATED,
                ActivityEntityType.SUBTASK,
                subTask.getId(),
                subTask.getSubtaskName(),
                String.format("{\"issueId\":\"%s\",\"issueName\":\"%s\"}",
                        issue.getId(), issue.getIssueName())
        );

        return toResponse(subTask);
    }

    /**
     * Updates an existing subtask's fields (name, description, completion status).
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param subtaskId the unique identifier of the subtask
     * @param request the fields to update
     * @return the updated SubTaskResponse DTO
     * @throws ForbiddenException if the user is not a project member
     * @throws ResourceNotFoundException if the project, issue, or subtask is not found
     */
    @Transactional
    public SubTaskResponse updateSubTask(UUID projectId, UUID issueId, UUID subtaskId, UpdateSubTaskRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Check project membership
        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        SubTask subTask = subTaskRepository.findByIdAndIssueId(subtaskId, issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));

        // Conditionally update and log modified fields
        if (request.getSubtaskName() != null) {
            subTask.setSubtaskName(request.getSubtaskName());
            activityService.log(project, currentUser,
                    ActivityAction.UPDATED, ActivityEntityType.SUBTASK,
                    subTask.getId(), subTask.getSubtaskName(),
                    String.format("{\"field\":\"name\",\"to\":\"%s\"}", request.getSubtaskName())
            );
        }

        if (request.getSubtaskDescription() != null) {
            subTask.setSubtaskDescription(request.getSubtaskDescription());
            activityService.log(project, currentUser,
                    ActivityAction.UPDATED, ActivityEntityType.SUBTASK,
                    subTask.getId(), subTask.getSubtaskName(),
                    String.format("{\"field\":\"description\",\"to\":\"%s\"}", request.getSubtaskDescription())
            );
        }

        if (request.getIsDone() != null) {
            subTask.setDone(request.getIsDone());
            activityService.log(project, currentUser,
                    request.getIsDone() ? ActivityAction.COMPLETED : ActivityAction.UPDATED,
                    ActivityEntityType.SUBTASK,
                    subTask.getId(), subTask.getSubtaskName(),
                    String.format("{\"field\":\"isDone\",\"to\":\"%s\"}", request.getIsDone())
            );
        }

        subTask.setUpdatedAt(Instant.now());
        subTaskRepository.save(subTask);

        return toResponse(subTask);
    }

    /**
     * Deletes a subtask. Only allowed for project OWNERs.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the parent issue
     * @param subtaskId the unique identifier of the subtask to delete
     * @throws ForbiddenException if user is not a project member or is not the project owner
     * @throws ResourceNotFoundException if the project, issue, or subtask is not found
     */
    public void deleteSubTask(UUID projectId, UUID issueId, UUID subtaskId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        // Verify the user is authorized to delete subtasks
        if (member.getRole() != ProjectRole.OWNER)
            throw new ForbiddenException("Only OWNER can delete subtask");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        SubTask subTask = subTaskRepository.findByIdAndIssueId(subtaskId, issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));

        // Log subtask deletion
        activityService.log(
                project,
                currentUser,
                ActivityAction.DELETED,
                ActivityEntityType.SUBTASK,
                subTask.getId(),
                subTask.getSubtaskName(),
                null
        );

        subTaskRepository.delete(subTask);
    }

    /**
     * Retrieves all subtasks belonging to a specific issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @return a list of SubTaskResponse DTOs
     * @throws ForbiddenException if current user is not a member of the project
     */
    public List<SubTaskResponse> getSubTasks(UUID projectId, UUID issueId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        return subTaskRepository.findByIssueId(issueId)
                .stream()
                .map(SubTaskService::toResponse)
                .toList();
    }

    /// Mapper
    private static SubTaskResponse toResponse(SubTask subTask) {
        return SubTaskResponse.builder()
                .id(subTask.getId())
                .subtaskName(subTask.getSubtaskName())
                .subtaskDescription(subTask.getSubtaskDescription())
                .isDone(subTask.isDone())
                .issueId(subTask.getIssue().getId())
                .createdAt(subTask.getCreatedAt())
                .updatedAt(subTask.getUpdatedAt())
                .build();
    }
}