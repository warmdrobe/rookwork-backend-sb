package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service class handling user notification tracking, retrieval, status updates (marking as read), and deletion.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;
    private final S3Service s3Service;

    /**
     * Retrieves all notifications for the current authenticated user.
     *
     * @return a list of NotificationResponse DTOs
     */
    public List<NotificationResponse> getAll() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves all unread notifications for the current authenticated user.
     *
     * @return a list of unread NotificationResponse DTOs
     */
    public List<NotificationResponse> getUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Counts the number of unread notifications for the current user.
     *
     * @return the count of unread notifications
     */
    public long countUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUserId);
    }

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId the unique identifier of the notification
     * @throws ResourceNotFoundException if the notification does not exist
     * @throws ForbiddenException if the notification does not belong to the current user
     */
    public void markAsRead(UUID notificationId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Verify the notification belongs to the current user before updating
        if (!notification.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your notification");

        notification.setRead(true);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    /**
     * Marks all unread notifications for the current user as read.
     */
    public void markAllAsRead() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserId);

        unread.forEach(n -> {
            n.setRead(true);
            n.setUpdatedAt(Instant.now());
        });
        notificationRepository.saveAll(unread);
    }

    /**
     * Deletes a specific notification.
     *
     * @param notificationId the unique identifier of the notification to delete
     * @throws ResourceNotFoundException if the notification is not found
     * @throws ForbiddenException if the notification does not belong to the current user
     */
    public void delete(UUID notificationId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Verify the notification belongs to the current user before deleting
        if (!notification.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your notification");

        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .sender(n.getSender() != null ? UserSummary.builder()
                        .id(n.getSender().getId())
                        .profileName(n.getSender().getProfileName())
                        .picture(s3Service.getAvatarUrl(n.getSender().getPicture()))
                        .build() : null)
                .message(n.getMessage())
                .issueId(n.getIssue() != null ? n.getIssue().getId() : null)
                .issueName(n.getIssue() != null ? n.getIssue().getIssueName() : null)
                .invitationId(n.getInvitation() != null ? n.getInvitation().getId() : null)
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}