package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.comments.CommentResponse;
import com.example.rookwork_backend_sb.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    /// Create comment

    /// Update comment

    /// Delete comment

    /// Get comment by post id

    /// Get comment by issue id
    @GetMapping("/{issueId}")
    public ResponseEntity<List<CommentResponse>> getAllCommentByIssueId ( @PathVariable UUID issueId){
        return ResponseEntity.ok(commentService.getAllCommentByIssueId(issueId));
    }
}
