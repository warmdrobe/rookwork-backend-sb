package com.example.rookwork_backend_sb.dtos;

import com.example.rookwork_backend_sb.entities.TicketStatus;
import lombok.Data;

@Data
public class SupportTicketReplyRequest {
    private TicketStatus status;
    private String reply;
}
