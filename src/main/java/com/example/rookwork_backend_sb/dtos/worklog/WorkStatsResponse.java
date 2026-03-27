package com.example.rookwork_backend_sb.dtos.worklog;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WorkStatsResponse {
    public List<DailyHours> thisWeek;
    public List<DailyHours> lastWeek;

    @Data
    @Builder
    public static class DailyHours {
        public String label;   // "Mon", "Tue", ...
        public BigDecimal hours;
    }
}