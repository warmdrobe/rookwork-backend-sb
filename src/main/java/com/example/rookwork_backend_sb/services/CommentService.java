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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

//import static com.example.rookwork_backend_sb.services.IssueService.getIssueResponse;

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
    /// Create comment
    @Transactional
    public CommentResponse createComment(UUID projectId, UUID issueId, CreateCommentRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Issue issue = issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        // Xử lý reply (nếu có parentCommentId)
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        commentRepository.save(comment);

        // Log activity
        activityService.log(
                project,
                currentUser,
                ActivityAction.COMMENTED,
                ActivityEntityType.COMMENT,
                comment.getId(),
                issue.getIssueName(),
                String.format("{\"preview\":\"%s\"}",
                        comment.getContent().length() > 50
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
                        .picture(currentUser.getPicture())
                        .build())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentCommentId(parent != null ? parent.getId() : null)
                .replies(Set.of())
                .build();

        // Broadcast tới tất cả đang xem issue
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_COMMENT");
        payload.put("comment", response);
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );

        // Notify assignee (nếu có và không phải người comment)
        if (issue.getAssignedTo() != null &&
                !issue.getAssignedTo().getId().equals(currentUserId)) {

            User assignee = issue.getAssignedTo();

            Notification notification = Notification.builder()
                    .user(assignee)
                    .issue(issue)
                    .title("New comment on your issue")
                    .message(String.format("%s commented on \"%s\" in project \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName(),
                            project.getProjectName()))
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
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

        // Notify người tạo issue (nếu không phải người comment và không trùng assignee)
        if (issue.getCreatedBy() != null &&
                !issue.getCreatedBy().getId().equals(currentUserId) &&
                (issue.getAssignedTo() == null ||
                        !issue.getCreatedBy().getId().equals(issue.getAssignedTo().getId()))) {

            User issueCreator = issue.getCreatedBy();

            Notification notification = Notification.builder()
                    .user(issueCreator)
                    .issue(issue)
                    .title("New comment on your issue")
                    .message(String.format("%s commented on \"%s\" in project \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName(),
                            project.getProjectName()))
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
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

        // Notify người viết comment gốc (nếu là reply, không trùng assignee/creator)
        if (parent != null &&
                !parent.getUser().getId().equals(currentUserId) &&
                (issue.getAssignedTo() == null ||
                        !parent.getUser().getId().equals(issue.getAssignedTo().getId())) &&
                (issue.getCreatedBy() == null ||
                        !parent.getUser().getId().equals(issue.getCreatedBy().getId()))) {

            User parentAuthor = parent.getUser();

            Notification notification = Notification.builder()
                    .user(parentAuthor)
                    .issue(issue)
                    .title("Someone replied to your comment")
                    .message(String.format("%s replied to your comment on \"%s\"",
                            currentUser.getProfileName(),
                            issue.getIssueName()))
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
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
    /// Update comment
    @Transactional
    public CommentResponse updateComment(UUID projectId, UUID issueId, UUID commentId, CreateCommentRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authentication"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Chỉ người viết comment mới được sửa
        if (!comment.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("You can only edit your own comment");

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // Broadcast update
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "UPDATED_COMMENT");
        payload.put("comment", getCommentResponse(comment));
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );

        return getCommentResponse(comment);
    }

    /// Delete comment
    @Transactional
    public void deleteComment(UUID projectId, UUID issueId, UUID commentId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Chỉ người viết comment hoặc OWNER mới được xóa
        ProjectMember currentMember = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        boolean isOwner = currentMember.getRole() == ProjectRole.OWNER;
        boolean isAuthor = comment.getUser().getId().equals(currentUserId);

        if (!isOwner && !isAuthor)
            throw new ForbiddenException("You can only delete your own comment");

        commentRepository.delete(comment);

        // Broadcast delete
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DELETED_COMMENT");
        payload.put("commentId", commentId);
        payload.put("issueId", issueId);
        simpMessagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/issue/" + issueId + "/comments",
                (Object) payload
        );
    }

    /// Get comment by project id
    public List<CommentResponse> getAllCommentByProjectId(UUID projectId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (!projectMemberRepository.existsById(new ProjectMemberId(currentUserId, projectId)))
            throw new ForbiddenException("Not a member of this project");

        return commentRepository.findByIssueProjectId(projectId)
                .stream()
                .map(CommentService::getCommentResponse)
                .toList();
    }

    /// Get comment by issue id
    public List<CommentResponse> getAllCommentByIssueId ( UUID issueId){
        UUID currentUserId = securityUtil.getCurrentUserId();

        return commentRepository.findByIssueId(issueId)
                .stream()
                .map(comment -> CommentResponse.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .issueId(comment.getIssue().getId())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .parentCommentId(comment.getParentComment()!= null
                                ? comment.getParentComment().getId()
                                : null)
                        .user(
                                UserSummary.builder()
                                        .id(comment.getUser().getId())
                                        .profileName(comment.getUser().getProfileName())
                                        .picture(comment.getUser().getPicture())
                                        .build()
                        )
                        .build())
                .toList();
    }

    /// Mapper
    private static CommentResponse getCommentResponse(Comment comment) {
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
            user.setPicture(comment.getUser().getPicture());

            response.setUser(user);
        }

        // map replies (recursive)
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            Set<CommentResponse> replies = comment.getReplies()
                    .stream()
                    .map(CommentService::getCommentResponse)
                    .collect(Collectors.toSet());

            response.setReplies(replies);
        }

        return response;
    }
}
