package com.example.rookwork_backend_sb.dtos.issues;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private UUID id;
    private String originalName;
    private String storedName;
    private String mimeType;
    private Integer sizeBytes;
    private String uploadedBy;
    private Instant createdAt;
    private String presignedUrl;
}
