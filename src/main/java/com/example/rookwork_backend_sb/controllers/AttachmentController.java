package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.issues.AttachmentResponse;
import com.example.rookwork_backend_sb.services.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/projects/{projectId}/issues/{issueId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @RequestParam("files") MultipartFile[] files) throws IOException {
        return ResponseEntity.ok(attachmentService.uploadAttachments(projectId, issueId, files));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @PathVariable UUID fileId) {
        attachmentService.deleteAttachment(projectId, issueId, fileId);
        return ResponseEntity.noContent().build();
    }
}
