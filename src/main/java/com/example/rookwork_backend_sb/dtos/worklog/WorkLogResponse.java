package com.example.rookwork_backend_sb.dtos.worklog;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WorkLogResponse {
    public UUID id;
    public UUID issueId;
    public String issueName;
    public String userProfileName;
    public String userPicture;
    public BigDecimal hours;
    public Instant loggedAt;
    public String note;
}