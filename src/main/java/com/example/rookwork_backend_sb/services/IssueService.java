package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.issues.*;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * Service class for managing project issues, subtasks, assignments, status changes, and related notifications.
 */
@AllArgsConstructor
@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final SecurityUtil securityUtil;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ActivityService activityService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    /**
     * Creates a new issue in a project and logs the creation activity.
     *
     * @param projectId the unique identifier of the project
     * @param request the issue creation request details
     * @return the created IssueResponse DTO
     * @throws ForbiddenException if the current user is not a member of the project
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ResourceNotFoundException if the project is not found
     */
    @Transactional
    public IssueResponse createIssue(UUID projectId, CreateIssueRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Verify the creator is a member of the project
        if(!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = Issue.builder()
                .issueName(request.getIssueName())
                .description(request.getDescription())
                .issueType(request.getIssueType())
                .priority(request.getPriority())
                .status(request.getStatus())
                .deadline(request.getDeadline())
                .project(project)
                .createdBy(currentUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        issueRepository.save(issue);

        // Log issue creation activity
        activityService.log(
                issue.getProject(),
                currentUser,
                ActivityAction.CREATED,
                ActivityEntityType.ISSUE,
                issue.getId(),
                issue.getIssueName(),
                issue,
                null
        );

        return getIssueResponse(projectId, issue);
    }

    /**
     * Updates an existing issue's fields, broadcasts the change to members, and logs activities.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param request the fields to update
     * @return the updated IssueResponse DTO
     * @throws ForbiddenException if the current user is not a project member
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ResourceNotFoundException if the project, issue, or assignee is not found
     * @throws BadRequestException if the update creates an invalid hierarchical dependency
     */
    public IssueResponse updateIssue(UUID projectId, UUID issueId, UpdateIssueRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Verify project membership
        if(!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository
                .findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        Status oldStatus = issue.getStatus();
        UUID oldAssigneeId = issue.getAssignedTo() != null ? issue.getAssignedTo().getId() : null;

        // Conditionally update updated fields
        if (request.getIssueName() != null) {
            issue.setIssueName(request.getIssueName());
        }

        if (request.getIssueType() != null) {
            issue.setIssueType(request.getIssueType());
        }

        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }

        if (request.getPriority() != null) {
            issue.setPriority(request.getPriority());
        }

        if (request.getDeadline() != null) {
            issue.setDeadline(request.getDeadline().atStartOfDay(ZoneOffset.UTC).toInstant());
        }

        // Parent issue validation
        if (request.getParentId() != null) {
            Issue parent = issueRepository
                    .findByIdAndProjectId(request.getParentId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent issue not found"));
            if (parent.getId().equals(issue.getId())) {
                throw new BadRequestException("Issue cannot be its own parent");
            }
            issue.setParent(parent);
        } else {
            issue.setParent(null);
        }

        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            issue.setAssignedTo(assignee);
        }

        if (request.getStatus() != null) {
            issue.setStatus(request.getStatus());
        }

        issue.setUpdatedAt(Instant.now());
        issueRepository.save(issue);

        // Broadcast the issue update via WebSocket to project subscribers
        String dest = "/topic/project/" + projectId + "/issues";
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ISSUE_UPDATED");
        payload.put("issue", getIssueResponse(projectId, issue));
        messagingTemplate.convertAndSend(dest, (Object) payload);

        // Handle assignee change activity log and push notifications
        if (request.getAssignedToId() != null
                && !request.getAssignedToId().equals(oldAssigneeId)
                && !request.getAssignedToId().equals(currentUserId))  {
            User assignee = issue.getAssignedTo();

            activityService.log(
                    project, currentUser,
                    ActivityAction.ASSIGNED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    issue,
                    Map.of(
                            "assigned_to_id", assignee.getId().toString(),
                            "assigned_to_name", assignee.getProfileName()
                    )
            );

            Notification notification = Notification.builder()
                    .user(assignee)
                    .sender(currentUser)
                    .issue(issue)
                    .title("You have been assigned to an issue")
                    .message(String.format("%s assigned you to \"%s\" in project \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName(),
                            project.getProjectName()))
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            notificationRepository.save(notification);

            messagingTemplate.convertAndSendToUser(
                    assignee.getId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "type", "ASSIGNED",
                            "notificationId", notification.getId(),
                            "issueId", issue.getId(),
                            "issueName", issue.getIssueName(),
                            "projectName", project.getProjectName(),
                            "assignedBy", currentUser.getProfileName()
                    )
            );
        }

        // Log status change activity
        if (request.getStatus() != null && request.getStatus() != oldStatus) {
            if (request.getStatus() == Status.DONE) {
                activityService.log(
                        project, currentUser,
                        ActivityAction.COMPLETED,
                        ActivityEntityType.ISSUE,
                        issue.getId(),
                        issue.getIssueName(),
                        issue,
                        null
                );
            } else {
                activityService.log(
                        project, currentUser,
                        ActivityAction.MOVED,
                        ActivityEntityType.ISSUE,
                        issue.getId(),
                        issue.getIssueName(),
                        issue,
                        Map.of("from", oldStatus.name(), "to", request.getStatus().name())
                );
            }
        }

        // Log other modifications (priority, name, description, deadline) if changed
        if (request.getPriority() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    issue,
                    Map.of("field", "priority", "to", request.getPriority().name())
            );
        }

        if (request.getIssueName() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    issue,
                    Map.of("field", "name", "to", request.getIssueName())
            );
        }

        if (request.getDescription() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    issue,
                    Map.of("field", "description", "to", request.getDescription())
            );
        }

        if (request.getDeadline() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    issue,
                    Map.of("field", "deadline", "to", request.getDeadline().toString())
            );
        }

        return getIssueResponse(projectId, issue);
    }

    /**
     * Deletes an issue. Only authorized for project OWNERs.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @throws ForbiddenException if current user is not a member or not the project owner
     * @throws ResourceNotFoundException if the issue is not found
     */
    @Transactional
    public void deleteIssue(UUID projectId, UUID issueId) {

        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of project"));

        // Only the project owner can delete issues
        if (member.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only OWNER can delete issue");
        }

        Issue issue = issueRepository
                .findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        // Log issue deletion activity (passing null for issue to prevent ON DELETE CASCADE from removing the log)
        activityService.log(
                issue.getProject(),
                currentUser,
                ActivityAction.DELETED,
                ActivityEntityType.ISSUE,
                issue.getId(),
                issue.getIssueName(),
                null,
                null
        );

        issueRepository.delete(issue);
    }

    /**
     * Retrieves all issues belonging to a project.
     *
     * @param projectId the unique identifier of the project
     * @return a list of IssueResponse DTOs
     * @throws ForbiddenException if user is not a project member
     */
    public List<IssueResponse> getAllIssue(UUID projectId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");
        return issueRepository.findAllByProjectId(projectId)
                .stream()
                .map(issue -> getIssueResponse(projectId, issue))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all issues assigned to the current user.
     *
     * @return a list of IssueResponse DTOs assigned to the user
     */
    public List<IssueResponse> getAllByAssignedToId() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return issueRepository.findAllByAssignedToId(currentUserId)
                .stream()
                .map(issue -> getIssueResponse(issue.getProject().getId(), issue))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details of a specific issue by its ID.
     *
     * @param issueId the unique identifier of the issue
     * @return the IssueResponse DTO
     * @throws ResourceNotFoundException if the issue is not found
     */
    public IssueResponse getIssueById(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        return getIssueResponse(issue.getProject().getId(), issue);
    }

    // issue mapping
    private static IssueResponse getIssueResponse(UUID projectId, Issue issue) {
        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setIssueName(issue.getIssueName());
        response.setDescription(issue.getDescription());
        response.setIssueType(issue.getIssueType());
        response.setPriority(issue.getPriority());
        response.setStatus(issue.getStatus());
        response.setParentId(issue.getParent() != null ? issue.getParent().getId() : null);
        response.setProjectId(projectId);
        response.setDeadline(issue.getDeadline());
        response.setCreatedAt(issue.getCreatedAt());
        response.setUpdatedAt(issue.getUpdatedAt());

        if (issue.getAssignedTo() != null) {
            UserSummary assignee = new UserSummary();
            assignee.setId(issue.getAssignedTo().getId());
            assignee.setProfileName(issue.getAssignedTo().getProfileName());
            assignee.setPicture(issue.getAssignedTo().getPicture());
            response.setAssignedTo(assignee);
        }

        return response;
    }
}
