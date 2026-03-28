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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;

    public List<NotificationResponse> getAll() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    public List<NotificationResponse> getUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    public long countUnread() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUserId);
    }

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

    public void delete(UUID notificationId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your notification");

        notificationRepository.delete(notification);
    }

    private static NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .sender(n.getSender() != null ? UserSummary.builder()
                        .id(n.getSender().getId())
                        .profileName(n.getSender().getProfileName())
                        .picture(n.getSender().getPicture())
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