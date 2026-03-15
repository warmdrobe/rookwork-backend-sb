package com.example.rookwork_backend_sb.dtos.notifications;

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
    private String title;
    private String message;
    private UUID issueId;
    private String issueName;
    private boolean isRead;
    private LocalDateTime createdAt;
}
