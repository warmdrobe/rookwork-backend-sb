package com.example.rookwork_backend_sb.dtos;

import com.example.rookwork_backend_sb.entities.TicketStatus;
import com.example.rookwork_backend_sb.entities.TicketType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SupportTicketResponse {
    private UUID id;
    private String subject;
    private String description;
    private TicketType type;
    private TicketStatus status;
    private String adminReply;
    private Instant createdAt;
    private Instant updatedAt;
    
    // For admin view
    private UUID userId;
    private String userEmail;
    private String userProfileName;
}
