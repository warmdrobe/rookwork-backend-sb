package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.dtos.comments.CreateCommentRequest;
import com.example.rookwork_backend_sb.entities.Comment;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.repositories.CommentRepository;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final IssueRepository issueRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;
    private final CommentRepository commentRepository;
    /// Create comment
//    public CommentResponse createComment(UUID issueId, CreateCommentRequest request){
//        UUID currentUserId = securityUtil.getCurrentUserId();
//
//    }
    /// Update comment

    /// Delete comment

    /// Get comment by post id

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
