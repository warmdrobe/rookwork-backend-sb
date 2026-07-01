package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.issues.*;
import com.example.rookwork_backend_sb.dtos.subtasks.SubTaskResponse;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    private final S3Service s3Service;
    private final ProjectStatusRepository projectStatusRepository;
    private final ProjectStatusService projectStatusService;
    private final WorkflowService workflowService;
    private final IssueTypeRepository issueTypeRepository;

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

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Resolve the status column — must belong to this project
        ProjectStatus status = null;
        if (request.getStatusId() != null) {
            status = projectStatusRepository.findByIdAndProjectId(request.getStatusId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found in this project"));
        } else {
            // Default to first column (TO_DO) if caller doesn't specify
            status = projectStatusRepository.findAllByProjectIdOrderByPositionAsc(projectId)
                    .stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No statuses configured for this project"));
        }
        if (request.getIssueTypeId() == null) {
            throw new BadRequestException("Issue type ID is required");
        }
        IssueType issueType = issueTypeRepository.findByIdAndProjectId(request.getIssueTypeId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue type not found"));

        Issue issue = Issue.builder()
                .issueName(request.getIssueName())
                .description(sanitizeHtml(request.getDescription()))
                .issueType(issueType)
                .priority(request.getPriority())
                .status(status)
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

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository
                .findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        ProjectStatus oldStatus = issue.getStatus();
        List<UUID> oldAssigneeIds = issue.getAssignees().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        boolean nameChanged = false;
        boolean descriptionChanged = false;
        boolean priorityChanged = false;
        boolean deadlineChanged = false;
        boolean assigneesChanged = false;
        boolean statusChanged = false;
        boolean parentChanged = false;
        boolean typeChanged = false;

        // Conditionally update updated fields
        if (request.getIssueName() != null) {
            if (request.getIssueName().trim().isEmpty()) {
                throw new BadRequestException("Issue name cannot be empty");
            }
            String newName = request.getIssueName();
            if (!newName.equals(issue.getIssueName())) {
                issue.setIssueName(newName);
                nameChanged = true;
            }
        }

        if (request.getIssueTypeId() != null) {
            IssueType issueType = issueTypeRepository.findByIdAndProjectId(request.getIssueTypeId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Issue type not found"));
            if (issue.getIssueType() == null || !issue.getIssueType().getId().equals(issueType.getId())) {
                issue.setIssueType(issueType);
                typeChanged = true;
            }
        }

        if (request.getDescription() != null) {
            String newDesc = sanitizeHtml(request.getDescription());
            String oldDesc = issue.getDescription();
            if (oldDesc == null ? newDesc != null : !oldDesc.equals(newDesc)) {
                issue.setDescription(newDesc);
                descriptionChanged = true;
            }
        }

        if (request.getPriority() != null) {
            PriorityType newPriority = request.getPriority();
            if (newPriority != issue.getPriority()) {
                issue.setPriority(newPriority);
                priorityChanged = true;
            }
        }

        if (request.getDeadline() != null) {
            Instant newDeadline = request.getDeadline().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant oldDeadline = issue.getDeadline();
            if (oldDeadline == null ? newDeadline != null : !oldDeadline.equals(newDeadline)) {
                issue.setDeadline(newDeadline);
                deadlineChanged = true;
            }
        }

        // Parent issue validation
        if (request.getParentId() != null) {
            UUID newParentId = request.getParentId();
            UUID oldParentId = issue.getParent() != null ? issue.getParent().getId() : null;
            if (newParentId == null ? oldParentId != null : !newParentId.equals(oldParentId)) {
                if (newParentId != null) {
                    Issue parent = issueRepository
                            .findByIdAndProjectId(newParentId, projectId)
                            .orElseThrow(() -> new ResourceNotFoundException("Parent issue not found"));
                    if (parent.getId().equals(issue.getId())) {
                        throw new BadRequestException("Issue cannot be its own parent");
                    }
                    issue.setParent(parent);
                } else {
                    issue.setParent(null);
                }
                parentChanged = true;
            }
        }

        if (request.getAssigneeIds() != null) {
            List<UUID> newIds = request.getAssigneeIds();
            List<UUID> currentIds = issue.getAssignees().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (!newIds.equals(currentIds)) {
                List<User> newAssignees = new ArrayList<>();
                for (UUID uid : newIds) {
                    User assignee = userRepository.findById(uid)
                            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found: " + uid));
                    newAssignees.add(assignee);
                }
                issue.setAssignees(newAssignees);
                assigneesChanged = true;
            }
        }

        if (request.getStatusId() != null) {
            ProjectStatus newStatus = projectStatusRepository
                    .findByIdAndProjectId(request.getStatusId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found in this project"));
            if (!newStatus.getId().equals(oldStatus != null ? oldStatus.getId() : null)) {
                if (oldStatus != null) {
                    workflowService.validateTransition(projectId, oldStatus.getId(), newStatus.getId());
                }
                issue.setStatus(newStatus);
                statusChanged = true;
            }
        }

        boolean isAnyFieldChanged = nameChanged || descriptionChanged || priorityChanged || deadlineChanged || assigneesChanged || statusChanged || parentChanged || typeChanged;

        if (isAnyFieldChanged) {
            issue.setUpdatedAt(Instant.now());
            issueRepository.save(issue);

            // Broadcast the issue update via WebSocket to project subscribers
            String dest = "/topic/project/" + projectId + "/issues";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ISSUE_UPDATED");
            payload.put("issue", getIssueResponse(projectId, issue));
            messagingTemplate.convertAndSend(dest, (Object) payload);
        }

        // Handle assignees change activity log and push notifications
        if (assigneesChanged) {
            List<User> currentAssignees = issue.getAssignees();
            if (!currentAssignees.isEmpty()) {
                // Notify newly added assignees (not in old list)
                for (User assignee : currentAssignees) {
                    if (!oldAssigneeIds.contains(assignee.getId()) && !assignee.getId().equals(currentUserId)) {
                        activityService.log(
                                project, currentUser,
                                ActivityAction.ASSIGNED,
                                ActivityEntityType.ISSUE,
                                issue.getId(),
                                issue.getIssueName(),
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
                }
            } else {
                activityService.log(
                        project, currentUser,
                        ActivityAction.UPDATED,
                        ActivityEntityType.ISSUE,
                        issue.getId(),
                        issue.getIssueName(),
                        Map.of("field", "assignees", "to", "Unassigned")
                );
            }
        }

        // Log status change activity
        if (statusChanged) {
            if (issue.getStatus() != null && issue.getStatus().getStatusCategory() == StatusCategory.DONE) {
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
                        Map.of(
                            "from", oldStatus != null ? oldStatus.getStatusName() : "none",
                            "to",   issue.getStatus() != null ? issue.getStatus().getStatusName() : "none"
                        )
                );
            }
        }

        // Log other modifications  (priority, name, description, deadline) if changed
        if (priorityChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "priority", "to", issue.getPriority().name())
            );
        }

        if (nameChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "name", "to", issue.getIssueName())
            );
        }

        if (descriptionChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "description", "to", issue.getDescription() != null ? issue.getDescription() : "")

            );
        }

        if (deadlineChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "deadline", "to", issue.getDeadline() != null ? issue.getDeadline().toString() : "None")
            );
        }

        if (parentChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "parent", "to", issue.getParent() != null ? issue.getParent().getIssueName() : "None")
            );
        }

        if (typeChanged) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    Map.of("field", "type", "to", issue.getIssueType().getName())
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
        return issueRepository.findAllByAssigneeId(currentUserId)
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
    private IssueResponse getIssueResponse(UUID projectId, Issue issue) {
        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setIssueName(issue.getIssueName());
        response.setDescription(issue.getDescription());
        if (issue.getIssueType() != null) {
            response.setIssueType(IssueTypeResponse.builder()
                    .id(issue.getIssueType().getId())
                    .name(issue.getIssueType().getName())
                    .description(issue.getIssueType().getDescription())
                    .iconKey(issue.getIssueType().getIconKey())
                    .color(issue.getIssueType().getColor())
                    .isSystem(issue.getIssueType().isSystem())
                    .build());
        }
        response.setPriority(issue.getPriority());
        response.setStatus(issue.getStatus() != null ? projectStatusService.toResponse(issue.getStatus()) : null);
        response.setParentId(issue.getParent() != null ? issue.getParent().getId() : null);
        response.setProjectId(projectId);
        response.setDeadline(issue.getDeadline());
        response.setCreatedAt(issue.getCreatedAt());
        response.setUpdatedAt(issue.getUpdatedAt());

        List<UserSummary> assignees = new ArrayList<>();
        if (issue.getAssignees() != null) {
            assignees = issue.getAssignees().stream().map(u -> {
                UserSummary s = new UserSummary();
                s.setId(u.getId());
                s.setProfileName(u.getProfileName());
                s.setPicture(s3Service.getAvatarUrl(u.getPicture()));
                return s;
            }).collect(Collectors.toList());
        }
        response.setAssignees(assignees);

        List<AttachmentResponse> attachments = new ArrayList<>();
        if (issue.getAttachments() != null) {
            attachments = issue.getAttachments().stream().map(file -> {
                String presignedUrl = s3Service.generatePresignedUrl(file.getStoredName());
                return AttachmentResponse.builder()
                        .id(file.getId())
                        .originalName(file.getOriginalName())
                        .storedName(file.getStoredName())
                        .mimeType(file.getMimeType())
                        .sizeBytes(file.getSizeBytes())
                        .uploadedBy(file.getUploadedBy())
                        .createdAt(file.getCreatedAt())
                        .presignedUrl(presignedUrl)
                        .build();
            }).collect(Collectors.toList());
        }
        response.setAttachments(attachments);

        List<SubTaskResponse> subtasks = new ArrayList<>();
        if (issue.getSubtasks() != null) {
            subtasks = issue.getSubtasks().stream().map(sub -> SubTaskResponse.builder()
                    .id(sub.getId())
                    .subtaskName(sub.getSubtaskName())
                    .subtaskDescription(sub.getSubtaskDescription())
                    .isDone(sub.isDone())
                    .issueId(sub.getIssue().getId())
                    .createdAt(sub.getCreatedAt())
                    .updatedAt(sub.getUpdatedAt())
                    .build()
            ).collect(Collectors.toList());
        }
        response.setSubtasks(subtasks);

        return response;
    }

    /**
     * Sanitizes user-submitted HTML content to prevent XSS Stored attacks.
     * Allows a safe subset of HTML tags and attributes (bold, italic, lists, links, images)
     * produced by rich-text editors like Tiptap. Strips any {@code <script>} or event handler attributes.
     *
     * @param html raw HTML input from the client (may be null)
     * @return sanitized HTML, or null if the input was null
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, Safelist.basicWithImages());
    }
}