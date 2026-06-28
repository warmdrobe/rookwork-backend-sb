package com.example.rookwork_backend_sb.dtos.worklog;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class LogWorkRequest {
    public UUID issueId;
    public Instant startAt;
    public Instant endAt;
    public String timezone;
    public String note;
}