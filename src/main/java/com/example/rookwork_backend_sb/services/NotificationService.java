package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.Dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.repositories.*;
import lombok.AllArgsConstructor;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class NotificationService {
    private final IssueRepository issueRepository;
    private final SecurityUtil securityUtil;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ActivityService activityService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
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
