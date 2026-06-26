package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.dtos.comments.CreateCommentRequest;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

//import static com.example.rookwork_backend_sb.services.IssueService.getIssueResponse;

/**
 * Service class handling comment creation, modification, deletion, and real-time broadcasting.
 */
@Service
@RequiredArgsConstructor
public class CommentService {
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final S3Service s3Service;
    /**
     * Creates a new comment under an issue, logs activity, and sends notifications.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param request the comment request data
     * @return the created CommentResponse DTO
     * @throws ForbiddenException if the user is not a member of the project
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ResourceNotFoundException if project, issue, or parent comment is not found
     */
    @Transactional
    public CommentResponse createComment(UUID projectId, UUID issueId, CreateCommentRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        // Resolve parent comment for replies
        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .issue(issue)
                .user(currentUser)
                .parentComment(parent)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        commentRepository.save(comment);

        // Log comment activity
        activityService.log(
                project,
                currentUser,
                ActivityAction.COMMENTED,
                ActivityEntityType.COMMENT,
                comment.getId(),
                issue.getIssueName(),
                issue,
                Map.of("preview", comment.getContent().length() > 50
                        ? comment.getContent().substring(0, 50) + "..."
                        : comment.getContent())
        );

        CommentResponse response = CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .issueId(issueId)
                .user(UserSummary.builder()
                        .id(currentUser.getId())
                        .profileName(currentUser.getProfileName())
                        .picture(s3Service.getAvatarUrl(currentUser.getPicture()))
                        .build())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentCommentId(parent != null ? parent.getId() : null)
                .replies(Set.of())
                .build();

        // Broadcast the new comment via WebSocket to all users viewing the issue
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_COMMENT");
        payload.put("comment", response);
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );

        // Notify each assignee if the comment was written by someone else
        for (User assignee : issue.getAssignees()) {
            if (!assignee.getId().equals(currentUserId)) {
                Notification notification = Notification.builder()
                        .user(assignee)
                        .sender(currentUser)
                        .issue(issue)
                        .title("New comment on your issue")
                        .message(String.format("%s commented on \"%s\" in project \"%s\"",
                                 currentUser.getProfileName(),
                                 issue.getIssueName(),
                                 project.getProjectName()))
                        .isRead(false)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                notificationRepository.save(notification);

                simpMessagingTemplate.convertAndSendToUser(
                        assignee.getId().toString(),
                        "/queue/notifications",
                        Map.of(
                                "type", "NEW_COMMENT",
                                "notificationId", notification.getId(),
                                "issueId", issue.getId(),
                                "issueName", issue.getIssueName(),
                                "projectName", project.getProjectName(),
                                "commentBy", currentUser.getProfileName()
                        )
                );
            }
        }

        // Notify the issue creator if they are not the author and not already an assignee
        List<UUID> assigneeIds = issue.getAssignees().stream()
                .map(User::getId).collect(java.util.stream.Collectors.toList());
        if (issue.getCreatedBy() != null &&
                !issue.getCreatedBy().getId().equals(currentUserId) &&
                !assigneeIds.contains(issue.getCreatedBy().getId())) {

            User issueCreator = issue.getCreatedBy();

            Notification notification = Notification.builder()
                    .user(issueCreator)
                    .sender(currentUser)
                    .issue(issue)
                    .title("New comment on your issue")
                    .message(String.format("%s commented on \"%s\" in project \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName(),
                            project.getProjectName()))
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            notificationRepository.save(notification);

            simpMessagingTemplate.convertAndSendToUser(
                    issueCreator.getId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "type", "NEW_COMMENT",
                            "notificationId", notification.getId(),
                            "issueId", issue.getId(),
                            "issueName", issue.getIssueName(),
                            "projectName", project.getProjectName(),
                            "commentBy", currentUser.getProfileName()
                    )
            );
        }

        // Notify the author of the parent comment if this is a reply
        if (parent != null &&
                !parent.getUser().getId().equals(currentUserId) &&
                !assigneeIds.contains(parent.getUser().getId()) &&
                (issue.getCreatedBy() == null ||
                        !parent.getUser().getId().equals(issue.getCreatedBy().getId()))) {

            User parentAuthor = parent.getUser();

            Notification notification = Notification.builder()
                    .user(parentAuthor)
                    .sender(currentUser)
                    .issue(issue)
                    .title("Someone replied to your comment")
                    .message(String.format("%s replied to your comment on \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName()))
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            notificationRepository.save(notification);

            simpMessagingTemplate.convertAndSendToUser(
                    parentAuthor.getId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "type", "REPLY_COMMENT",
                            "notificationId", notification.getId(),
                            "issueId", issue.getId(),
                            "issueName", issue.getIssueName(),
                            "projectName", project.getProjectName(),
                            "replyBy", currentUser.getProfileName()
                    )
            );
        }

        return response;
    }
    /**
     * Updates the content of an existing comment.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param commentId the unique identifier of the comment to update
     * @param request the updated comment data
     * @return the updated CommentResponse DTO
     * @throws ForbiddenException if user is not a project member or is not the author of the comment
     * @throws ResourceNotFoundException if the comment is not found
     */
    @Transactional
    public CommentResponse updateComment(UUID projectId, UUID issueId, UUID commentId, CreateCommentRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Path consistency check: verify comment belongs to this issue and project
        if (!comment.getIssue().getId().equals(issueId) || !comment.getIssue().getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Comment does not belong to this issue or project");
        }

        // Verify that only the original author can edit the comment
        if (!comment.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("You can only edit your own comment");

        comment.setContent(request.getContent());
        comment.setUpdatedAt(Instant.now());
        commentRepository.save(comment);

        // Broadcast updated comment via WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "UPDATED_COMMENT");
        payload.put("comment", getCommentResponse(comment));
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );

        return getCommentResponse(comment);
    }

    /**
     * Deletes a comment. Only the author or a project owner is authorized.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param commentId the unique identifier of the comment to delete
     * @throws ForbiddenException if user is not authorized to delete the comment
     * @throws ResourceNotFoundException if the comment is not found
     */
    @Transactional
    public void deleteComment(UUID projectId, UUID issueId, UUID commentId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Path consistency check: verify comment belongs to this issue and project
        if (!comment.getIssue().getId().equals(issueId) || !comment.getIssue().getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Comment does not belong to this issue or project");
        }

        ProjectMember currentMember = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        // Only the comment author or project OWNER is allowed to delete a comment
        boolean isOwner = currentMember.getRole() == ProjectRole.OWNER;
        boolean isAuthor = comment.getUser().getId().equals(currentUserId);

        if (!isOwner && !isAuthor)
            throw new ForbiddenException("You can only delete your own comment");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        // Log comment deletion activity
        activityService.log(
                comment.getIssue().getProject(),
                currentUser,
                ActivityAction.DELETED,
                ActivityEntityType.COMMENT,
                comment.getId(),
                comment.getIssue().getIssueName(),
                comment.getIssue(),
                null
        );

        commentRepository.delete(comment);

        // Broadcast deleted comment ID via WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DELETED_COMMENT");
        payload.put("commentId", commentId);
        payload.put("issueId", issueId);
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );
    }

    /**
     * Retrieves all comments associated with a project.
     *
     * @param projectId the unique identifier of the project
     * @return a list of CommentResponse DTOs for the project
     * @throws ForbiddenException if the current user is not a member of the project
     */
    public List<CommentResponse> getAllCommentByProjectId(UUID projectId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        return commentRepository.findByIssueProjectId(projectId)
                .stream()
                .map(this::getCommentResponse)
                .toList();
    }

    /**
     * Retrieves all root comments (replies nested recursively) for a specific issue.
     *
     * @param projectId the unique identifier of the project (for path consistency check)
     * @param issueId the unique identifier of the issue
     * @return a list of top-level CommentResponse DTOs
     */
    public List<CommentResponse> getAllCommentByIssueId(UUID projectId, UUID issueId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        // Path consistency check: verify issue actually belongs to this project
        issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found in this project"));
        return commentRepository.findByIssueIdAndParentCommentIsNull(issueId)
                .stream()
                .map(this::getCommentResponse)
                .toList();
    }

    /// Mapper
    private CommentResponse getCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();

        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setIssueId(comment.getIssue().getId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        response.setParentCommentId(
                comment.getParentComment() != null
                        ? comment.getParentComment().getId()
                        : null
        );

        // map user
        if (comment.getUser() != null) {
            UserSummary user = new UserSummary();
            user.setId(comment.getUser().getId());
            user.setProfileName(comment.getUser().getProfileName());
            user.setPicture(s3Service.getAvatarUrl(comment.getUser().getPicture()));

            response.setUser(user);
        }

        // map replies (recursive)
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            Set<CommentResponse> replies = comment.getReplies()
                    .stream()
                    .map(this::getCommentResponse)
                    .collect(Collectors.toSet());

            response.setReplies(replies);
        }

        return response;
    }
}
