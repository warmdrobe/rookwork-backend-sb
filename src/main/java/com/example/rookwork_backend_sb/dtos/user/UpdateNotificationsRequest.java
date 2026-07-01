package com.example.rookwork_backend_sb.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNotificationsRequest {
    private boolean notifyIssueAssigned;
    private boolean notifyMentioned;
    private boolean notifyProjectUpdates;
    private boolean notifyDailyDigest;
    private boolean notifyComment;
    private boolean notifyEventInvited;
}
