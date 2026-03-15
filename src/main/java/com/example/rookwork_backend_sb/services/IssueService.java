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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

    // create issue
    @Transactional
    public IssueResponse createIssue(UUID projectId, CreateIssueRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        issueRepository.save(issue);

        activityService.log(
                issue.getProject(),
                currentUser,
                ActivityAction.CREATED,
                ActivityEntityType.ISSUE,
                issue.getId(),
                issue.getIssueName(),
                null
        );

        return getIssueResponse(projectId, issue);
    }

    // update issue
    public IssueResponse updateIssue(UUID projectId, UUID issueId, UpdateIssueRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

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

        if (request.getIssueName() != null) {
            issue.setIssueName(request.getIssueName());
        }

        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }

        if (request.getPriority() != null) {
            issue.setPriority(request.getPriority());
        }

        if (request.getDeadline() != null) {
            issue.setDeadline(request.getDeadline().atStartOfDay());
        }

        if (request.getParentId() != null) {
            Issue parent = issueRepository
                    .findByIdAndProjectId(request.getParentId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent issue not found"));
            if (parent.getId().equals(issue.getId())) {
                throw new BadRequestException("Issue cannot be its own parent");
            }
            issue.setParent(parent);
        }

        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            issue.setAssignedTo(assignee);
        }

        if (request.getStatus() != null) {
            issue.setStatus(request.getStatus());
        }

        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);

        // Broadcast issue update tới tất cả member đang xem project (để test WS + real-time UI)
        String dest = "/topic/project/" + projectId + "/issues";
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ISSUE_UPDATED");
        payload.put("issue", getIssueResponse(projectId, issue));
        messagingTemplate.convertAndSend(dest, (Object) payload);

        // Log + notify khi assign thay đổi
        if (request.getAssignedToId() != null && !request.getAssignedToId().equals(oldAssigneeId)) {
            User assignee = issue.getAssignedTo();

            activityService.log(
                    project, currentUser,
                    ActivityAction.ASSIGNED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    String.format("{\"assigned_to_id\":\"%s\",\"assigned_to_name\":\"%s\"}",
                            assignee.getId(),
                            assignee.getProfileName())
            );

            Notification notification = Notification.builder()
                    .user(assignee)
                    .issue(issue)
                    .title("You have been assigned to an issue")
                    .message(String.format("%s assigned you to \"%s\" in project \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName(),
                            project.getProjectName()))
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
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

        // Log status changed
        if (request.getStatus() != null && request.getStatus() != oldStatus) {
            if (request.getStatus() == Status.DONE) {
                activityService.log(
                        project, currentUser,
                        ActivityAction.COMPLETED,
                        ActivityEntityType.ISSUE,
                        issue.getId(),
                        issue.getIssueName(),
                        null
                );
            } else {
                activityService.log(
                        project, currentUser,
                        ActivityAction.MOVED,
                        ActivityEntityType.ISSUE,
                        issue.getId(),
                        issue.getIssueName(),
                        String.format("{\"from\":\"%s\",\"to\":\"%s\"}", oldStatus, request.getStatus())
                );
            }
        }

        // Log priority changed
        if (request.getPriority() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    String.format("{\"field\":\"priority\",\"to\":\"%s\"}", request.getPriority())
            );
        }

        // Log name changed
        if (request.getIssueName() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    String.format("{\"field\":\"name\",\"to\":\"%s\"}", request.getIssueName())
            );
        }

        // Log description changed
        if (request.getDescription() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    String.format("{\"field\":\"description\",\"to\":\"%s\"}", request.getDescription())
            );
        }

        // Log deadline changed
        if (request.getDeadline() != null) {
            activityService.log(
                    project, currentUser,
                    ActivityAction.UPDATED,
                    ActivityEntityType.ISSUE,
                    issue.getId(),
                    issue.getIssueName(),
                    String.format("{\"field\":\"deadline\",\"to\":\"%s\"}", request.getDeadline())
            );
        }

        return getIssueResponse(projectId, issue);
    }

    //delete
    public void deleteIssue(UUID projectId, UUID issueId) {

        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember member = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of project"));

        if (member.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only OWNER can delete issue");
        }

        Issue issue = issueRepository
                .findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        issueRepository.delete(issue);
    }

    // get all
    public List<IssueResponse> getAllIssue(UUID projectId) {
        UUID currentUserId= securityUtil.getCurrentUserId();
        if(!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");
        return issueRepository.findAllByProjectId(projectId)
                .stream()
                .map(issue -> IssueResponse.builder()
                        .id(issue.getId())
                        .issueName(issue.getIssueName())
                        .issueType(issue.getIssueType())
                        .priority(issue.getPriority())
                        .status(issue.getStatus())
                        .deadline(issue.getDeadline())
                        .build())
                .collect(Collectors.toList());
    }

    // get issue by user id
    public List<IssueResponse> getAllByAssignedToId() {
        UUID currentUserId= securityUtil.getCurrentUserId();
        return issueRepository.findAllByAssignedToId(currentUserId)
                .stream()
                .map(issue -> IssueResponse.builder()
                        .id(issue.getId())
                        .issueName(issue.getIssueName())
                        .description(issue.getDescription())
                        .issueType(issue.getIssueType())
                        .priority(issue.getPriority())
                        .status(issue.getStatus())
                        .deadline(issue.getDeadline())
                        .build())
                .collect(Collectors.toList());
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
