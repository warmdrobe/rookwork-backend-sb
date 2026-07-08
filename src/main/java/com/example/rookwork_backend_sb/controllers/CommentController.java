package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.comments.CommentReactionResponse;
import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.dtos.comments.CreateCommentRequest;
import com.example.rookwork_backend_sb.dtos.comments.ReactCommentRequest;
import com.example.rookwork_backend_sb.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for comment CRUD operations under issues and projects.
 */
@RestController
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
@PreAuthorize("@projectSecurity.isMember(#projectId)")
public class CommentController {
    private final CommentService commentService;

    /**
     * Creates a new comment under an issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param request the comment details payload
     * @return response entity containing the created CommentResponse DTO
     */
    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(projectId, issueId, request));
    }

    /**
     * Updates an existing comment.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param commentId the unique identifier of the comment to update
     * @param request the updated comment content payload
     * @return response entity containing the updated CommentResponse DTO
     */
    @PutMapping("/issues/{issueId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(projectId, issueId, commentId, request));
    }

    /**
     * Deletes a comment.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param commentId the unique identifier of the comment to delete
     * @return response entity with no content
     */
    @DeleteMapping("/issues/{issueId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID commentId) {
        commentService.deleteComment(projectId, issueId, commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all comments for a project.
     *
     * @param projectId the unique identifier of the project
     * @return response entity containing a list of CommentResponse DTOs
     */
    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponse>> getAllCommentByProjectId(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(commentService.getAllCommentByProjectId(projectId));
    }

    /**
     * Retrieves all comments for an issue.
     *
     * @param issueId the unique identifier of the issue
     * @return response entity containing a list of top-level CommentResponse DTOs (with nested replies)
     */
    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<CommentResponse>> getAllCommentByIssueId(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(commentService.getAllCommentByIssueId(projectId, issueId));
    }

    /**
     * Thả / đổi / gỡ biểu cảm (reaction) trên một bình luận.
     * Cơ chế toggle: click cùng emoji sẽ gỡ bỏ, click emoji khác sẽ đổi sang loại mới.
     *
     * @param projectId ID của dự án
     * @param issueId   ID của công việc
     * @param commentId ID của bình luận
     * @param request   Payload chứa reactionType (chuỗi emoji)
     * @return Danh sách reactions tổng hợp mới nhất của bình luận đó
     */
    @PostMapping("/issues/{issueId}/comments/{commentId}/reactions")
    public ResponseEntity<List<CommentReactionResponse>> reactToComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @RequestBody ReactCommentRequest request) {
        return ResponseEntity.ok(commentService.reactToComment(projectId, issueId, commentId, request));
    }
}