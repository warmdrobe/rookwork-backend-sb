package com.example.rookwork_backend_sb.dtos;

import com.example.rookwork_backend_sb.entities.TicketType;
import lombok.Data;

@Data
public class SupportTicketRequest {
    private String subject;
    private String description;
    private TicketType type;
}
