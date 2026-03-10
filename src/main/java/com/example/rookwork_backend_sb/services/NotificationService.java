package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.repositories.*;
import lombok.AllArgsConstructor;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class NotificationService {
    private final SecurityUtil securityUtil;
    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getAll() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return notificationRepository.findAllByUser_Id(currentUserId)
                .stream()
                .map(notification -> NotificationResponse.builder()
                        .id(notification.getId())
                        .userId(notification.getUser().getId())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());
    }
}
