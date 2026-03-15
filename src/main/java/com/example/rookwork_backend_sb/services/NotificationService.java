package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;

    /// Get all notifications
    public List<NotificationResponse> getAll() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    /// Get unread notifications
    public List<NotificationResponse> getUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    /// Count unread
    public long countUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUserId);
    }

    /// Mark one as read
    public void markAsRead(UUID notificationId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your notification");

        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /// Mark all as read
    public void markAllAsRead() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserId);

        unread.forEach(n -> {
            n.setRead(true);
            n.setUpdatedAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }

    /// Mapper
    private static NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .issueId(n.getIssue().getId())
                .issueName(n.getIssue().getIssueName())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}