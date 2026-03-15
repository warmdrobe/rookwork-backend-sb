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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    /// Create subtask
    @Transactional
    public SubTaskResponse createSubTask(UUID projectId, UUID issueId, CreateSubTaskRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        subTaskRepository.save(subTask);

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

    /// Update subtask
    @Transactional
    public SubTaskResponse updateSubTask(UUID projectId, UUID issueId, UUID subtaskId, UpdateSubTaskRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

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

        subTask.setUpdatedAt(LocalDateTime.now());
        subTaskRepository.save(subTask);

        return toResponse(subTask);
    }

    /// Delete subtask
    public void deleteSubTask(UUID projectId, UUID issueId, UUID subtaskId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

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

    /// Get subtasks by issue
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