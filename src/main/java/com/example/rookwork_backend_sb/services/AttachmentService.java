package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.issues.AttachmentResponse;
import com.example.rookwork_backend_sb.entities.File;
import com.example.rookwork_backend_sb.entities.Issue;
import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.entities.ProjectRole;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.FileRepository;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final FileRepository fileRepository;
    private final IssueRepository issueRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final S3Service s3Service;

    /**
     * Uploads multiple files as attachments to an issue.
     */
    @Transactional
    public List<AttachmentResponse> uploadAttachments(UUID projectId, UUID issueId, MultipartFile[] files) throws IOException {
        UUID currentUserId = securityUtil.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        // Verify issue belongs to the project
        if (!issue.getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Issue does not belong to this project");
        }

        // Calculate incoming files total size
        long incomingSize = 0;
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    incomingSize += file.getSize();
                }
            }
        }

        // Check if project upload limit (2GB) is exceeded
        long currentTotalSize = fileRepository.sumSizeBytesByProjectId(projectId);
        long maxLimit = 2L * 1024 * 1024 * 1024; // 2GB
        if (currentTotalSize + incomingSize > maxLimit) {
            throw new BadRequestException("Project upload limit exceeded. Maximum project storage limit is 2GB.");
        }

        List<AttachmentResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Upload to S3
            String storedName = s3Service.uploadFile(file, projectId, issueId);

            // Save database record
            File attachment = File.builder()
                    .originalName(file.getOriginalFilename())
                    .storedName(storedName)
                    .mimeType(file.getContentType())
                    .sizeBytes((int) file.getSize())
                    .storagePath(storedName)
                    .uploadedBy(currentUser.getProfileName())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .user(currentUser)
                    .issue(issue)
                    .build();

            fileRepository.save(attachment);

            // Generate response DTO with presigned URL
            String presignedUrl = s3Service.generatePresignedUrl(storedName);
            responses.add(mapToResponse(attachment, presignedUrl));
        }

        return responses;
    }

    /**
     * Deletes a specific file attachment from an issue.
     */
    @Transactional
    public void deleteAttachment(UUID projectId, UUID issueId, UUID fileId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Verify the file belongs to the specified issue and project
        if (file.getIssue() == null || !file.getIssue().getId().equals(issueId)) {
            throw new ForbiddenException("Attachment does not belong to this issue");
        }
        if (!file.getIssue().getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Attachment does not belong to this project");
        }

        // Verify current user is the uploader of the file or the project OWNER
        ProjectMember currentMember = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        boolean isOwner = currentMember.getRole() == ProjectRole.OWNER;
        boolean isUploader = file.getUser() != null && file.getUser().getId().equals(currentUserId);

        if (!isOwner && !isUploader) {
            throw new ForbiddenException("You are not authorized to delete this attachment");
        }

        // Delete from S3
        s3Service.deleteFile(file.getStoredName());

        // Delete from database
        fileRepository.delete(file);
    }

    /**
     * Gets all attachments for an issue with fresh presigned URLs.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsForIssue(UUID issueId) {
        List<File> files = fileRepository.findByIssueId(issueId);
        return files.stream().map(file -> {
            String presignedUrl = s3Service.generatePresignedUrl(file.getStoredName());
            return mapToResponse(file, presignedUrl);
        }).collect(Collectors.toList());
    }

    /**
     * Moves a file attachment to a different target issue.
     */
    @Transactional
    public AttachmentResponse moveAttachment(UUID projectId, UUID fileId, UUID targetIssueId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        // Verify source file belongs to the project
        if (file.getIssue() == null || !file.getIssue().getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Attachment does not belong to this project");
        }

        Issue targetIssue = issueRepository.findById(targetIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Target issue not found"));

        // Verify target issue belongs to the project
        if (!targetIssue.getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Target issue does not belong to this project");
        }

        file.setIssue(targetIssue);
        file.setUpdatedAt(Instant.now());
        File saved = fileRepository.save(file);

        String presignedUrl = s3Service.generatePresignedUrl(saved.getStoredName());
        return mapToResponse(saved, presignedUrl);
    }

    /**
     * Maps a File entity to AttachmentResponse DTO.
     */
    public AttachmentResponse mapToResponse(File file, String presignedUrl) {
        return AttachmentResponse.builder()
                .id(file.getId())
                .originalName(file.getOriginalName())
                .storedName(file.getStoredName())
                .mimeType(file.getMimeType())
                .sizeBytes(file.getSizeBytes())
                .uploadedBy(file.getUploadedBy())
                .createdAt(file.getCreatedAt())
                .presignedUrl(presignedUrl)
                .build();
    }
}
