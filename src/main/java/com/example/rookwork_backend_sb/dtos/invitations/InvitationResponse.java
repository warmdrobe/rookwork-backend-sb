package com.example.rookwork_backend_sb.dtos.invitations;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InvitationResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private UUID invitedById;
    private String invitedByName;
    private UUID invitedUserId;
    private String invitedUserName;
    private String invitedUserEmail;
    private String invitedUserPicture;
    private String status;
    private Instant createdAt;
}
