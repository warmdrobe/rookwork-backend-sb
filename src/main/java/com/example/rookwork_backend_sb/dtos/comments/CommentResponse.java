package com.example.rookwork_backend_sb.dtos.comments;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
  public Instant createdAt;
  public Instant updatedAt;
  public UUID parentCommentId;
  public Set<CommentResponse> replies;
}
