package com.example.rookwork_backend_sb.Dtos.invitations;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InvitationResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private UUID invitedById;
    private String invitedByName;
    private String status;
    private LocalDateTime createdAt;
}
