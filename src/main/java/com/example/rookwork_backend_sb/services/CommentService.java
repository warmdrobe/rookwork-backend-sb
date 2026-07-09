package com.example.rookwork_backend_sb.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.comments.CommentReactionResponse;
import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.dtos.comments.CreateCommentRequest;
import com.example.rookwork_backend_sb.dtos.comments.ReactCommentRequest;
import com.example.rookwork_backend_sb.entities.ActivityAction;
import com.example.rookwork_backend_sb.entities.ActivityEntityType;
import com.example.rookwork_backend_sb.entities.Comment;
import com.example.rookwork_backend_sb.entities.CommentReaction;
import com.example.rookwork_backend_sb.entities.Issue;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.entities.Project;
import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.entities.ProjectRole;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.CommentReactionRepository;
import com.example.rookwork_backend_sb.repositories.CommentRepository;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final S3Service s3Service;
    private final EmailService emailService;

    @Value("${app.frontend.url:https://www.rookwork.asia}")
    private String frontendUrl;
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
                .content(sanitizeHtml(request.getContent()))
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
                .reactions(List.of())
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

        comment.setContent(sanitizeHtml(request.getContent()));
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

        // map reactions: nhóm theo reactionType, đếm số lượng và lấy danh sách user
        List<CommentReaction> rawReactions = commentReactionRepository.findByCommentId(comment.getId());
        List<CommentReactionResponse> reactions = rawReactions.stream()
                .collect(Collectors.groupingBy(CommentReaction::getReactionType))
                .entrySet().stream()
                .map(entry -> {
                    List<UserSummary> users = entry.getValue().stream()
                            .map(r -> UserSummary.builder()
                                    .id(r.getUser().getId())
                                    .profileName(r.getUser().getProfileName())
                                    .picture(s3Service.getAvatarUrl(r.getUser().getPicture()))
                                    .build())
                            .collect(Collectors.toList());
                    return CommentReactionResponse.builder()
                            .reactionType(entry.getKey())
                            .count(users.size())
                            .users(users)
                            .build();
                })
                .collect(Collectors.toList());
        response.setReactions(reactions);

        return response;
    }

    /**
     * Xử lý thả / đổi / gỡ biểu cảm trên một bình luận.
     * Quy tắc: Mỗi người dùng chỉ được có tối đa 1 reaction type per comment.
     * - Nếu đã thả cùng loại emoji => gỡ bỏ (toggle off).
     * - Nếu đã thả emoji khác => cập nhật sang loại mới.
     * - Nếu chưa có => tạo mới.
     *
     * @param projectId ID của dự án
     * @param issueId   ID của công việc
     * @param commentId ID của bình luận
     * @param request   Payload chứa reactionType
     * @return Danh sách reactions mới nhất của bình luận đó
     */
    @Transactional
    public List<CommentReactionResponse> reactToComment(
            UUID projectId, UUID issueId, UUID commentId, ReactCommentRequest request) {

        UUID currentUserId = securityUtil.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Kiểm tra bình luận thuộc đúng issue và project
        if (!comment.getIssue().getId().equals(issueId)
                || !comment.getIssue().getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Comment does not belong to this issue or project");
        }

        String newType = request.getReactionType();
        final boolean[] shouldNotify = {false};

        commentReactionRepository.findByCommentIdAndUserId(commentId, currentUserId)
                .ifPresentOrElse(
                        existing -> {
                            if (existing.getReactionType().equals(newType)) {
                                // Toggle off: cùng loại emoji -> xóa
                                commentReactionRepository.delete(existing);
                            } else {
                                // Đổi sang emoji mới
                                existing.setReactionType(newType);
                                existing.setCreatedAt(Instant.now());
                                commentReactionRepository.save(existing);
                                shouldNotify[0] = true;
                            }
                        },
                        () -> {
                            // Chưa có reaction -> tạo mới
                            CommentReaction reaction = CommentReaction.builder()
                                    .comment(comment)
                                    .user(currentUser)
                                    .reactionType(newType)
                                    .createdAt(Instant.now())
                                    .build();
                            commentReactionRepository.save(reaction);
                            shouldNotify[0] = true;
                        }
                );

        // Notify comment author
        if (shouldNotify[0] && !comment.getUser().getId().equals(currentUserId)) {
            User commentAuthor = comment.getUser();
            String commentText = comment.getContent();
            String plainText = commentText != null ? Jsoup.parse(commentText).text() : "";
            String preview = plainText.length() > 30 ? plainText.substring(0, 30) + "..." : plainText;

            Notification notification = Notification.builder()
                    .user(commentAuthor)
                    .sender(currentUser)
                    .issue(comment.getIssue())
                    .title("New reaction on your comment")
                    .message(String.format("%s reacted %s to your comment: \"%s\"",
                            currentUser.getProfileName(),
                            newType,
                            preview))
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            notificationRepository.save(notification);

            // Send notification real-time via WebSocket
            simpMessagingTemplate.convertAndSendToUser(
                    commentAuthor.getId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "type", "COMMENT_REACTION",
                            "notificationId", notification.getId(),
                            "issueId", comment.getIssue().getId(),
                            "issueName", comment.getIssue().getIssueName(),
                            "projectName", comment.getIssue().getProject().getProjectName(),
                            "reactedBy", currentUser.getProfileName(),
                            "reactionType", newType
                    )
            );
        }

        // Tổng hợp danh sách reactions mới nhất
        List<CommentReaction> rawReactions = commentReactionRepository.findByCommentId(commentId);
        List<CommentReactionResponse> updatedReactions = rawReactions.stream()
                .collect(Collectors.groupingBy(CommentReaction::getReactionType))
                .entrySet().stream()
                .map(entry -> {
                    List<UserSummary> users = entry.getValue().stream()
                            .map(r -> UserSummary.builder()
                                    .id(r.getUser().getId())
                                    .profileName(r.getUser().getProfileName())
                                    .picture(s3Service.getAvatarUrl(r.getUser().getPicture()))
                                    .build())
                            .collect(Collectors.toList());
                    return CommentReactionResponse.builder()
                            .reactionType(entry.getKey())
                            .count(users.size())
                            .users(users)
                            .build();
                })
                .collect(Collectors.toList());

        // Phát tin nhắn WebSocket thông báo reactions đã được cập nhật
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "COMMENT_REACTION_UPDATED");
        payload.put("commentId", commentId.toString());
        payload.put("issueId", issueId.toString());
        payload.put("reactions", updatedReactions);
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );

        return updatedReactions;
    }

    /**
     * Sanitizes user-submitted HTML to prevent XSS Stored attacks.
     * Preserves safe rich-text formatting (bold, italic, links, images)
     * while stripping {@code <script>} tags and event handler attributes.
     *
     * @param html raw HTML input (may be null)
     * @return sanitized HTML, or null if input was null
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, Safelist.basicWithImages());
    }
}
