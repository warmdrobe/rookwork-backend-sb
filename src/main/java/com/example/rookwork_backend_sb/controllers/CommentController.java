package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.dtos.comments.CreateCommentRequest;
import com.example.rookwork_backend_sb.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
//@RequestMapping("api/comments")
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /// Create comment
    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(projectId, issueId, request));
    }

    /// Update comment
    @PutMapping("/issues/{issueId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(projectId, issueId, commentId, request));
    }

    /// Delete comment
    @DeleteMapping("/issues/{issueId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID commentId) {
        commentService.deleteComment(projectId, issueId, commentId);
        return ResponseEntity.noContent().build();
    }

    /// Get comment by project id
    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponse>> getAllCommentByProjectId(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(commentService.getAllCommentByProjectId(projectId));
    }

    /// Get comment by issue id
    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<CommentResponse>> getAllCommentByIssueId(
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(commentService.getAllCommentByIssueId(issueId));
    }
}