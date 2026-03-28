package com.example.rookwork_backend_sb.dtos.notifications;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UserSummary user;
    private UserSummary sender;
    private String title;
    private String message;
    private UUID issueId;
    private String issueName;
    private UUID invitationId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
