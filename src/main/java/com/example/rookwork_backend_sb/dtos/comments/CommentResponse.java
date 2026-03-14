package com.example.rookwork_backend_sb.dtos.comments;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.entities.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    public UUID id;
    public String content;
    public UUID issueId;
    public UserSummary user;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public UUID parentCommentId;
    public Set<CommentResponse> replies;
}
