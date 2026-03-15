package com.example.rookwork_backend_sb.dtos.comments;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {
    public String content;
    public UUID parentCommentId;
}
