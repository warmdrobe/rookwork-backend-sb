package com.example.rookwork_backend_sb.Dtos.invitations;

import lombok.Data;

import java.util.UUID;

@Data
public class SendInviteRequest {
    private UUID projectId;
    private String email;
}
