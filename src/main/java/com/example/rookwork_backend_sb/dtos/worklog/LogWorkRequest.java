package com.example.rookwork_backend_sb.dtos.worklog;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LogWorkRequest {
    public UUID issueId;
    public LocalDateTime startAt;
    public LocalDateTime endAt;
    public String note;
}