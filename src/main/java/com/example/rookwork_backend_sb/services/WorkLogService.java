package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.worklog.*;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.*;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;


    public List<WorkLogResponse> logWork(LogWorkRequest request) {
        UUID userId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Issue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        LocalDateTime start = request.getStartAt();
        LocalDateTime end = request.getEndAt();

        if (start == null || end == null)
            throw new BadRequestException("startAt and endAt are required");
        if (!end.isAfter(start))
            throw new BadRequestException("endAt must be after startAt");

        // Split thành các segments theo từng ngày
        List<WorkLog> logs = new ArrayList<>();
        LocalDateTime cursor = start;

        while (cursor.isBefore(end)) {
            // Cuối ngày hiện tại (23:59:59.999...)
            LocalDateTime dayEnd = cursor.toLocalDate().atTime(LocalTime.MAX);
            // Segment kết thúc tại dayEnd hoặc end, tùy cái nào sớm hơn
            LocalDateTime segmentEnd = dayEnd.isBefore(end) ? dayEnd : end;

            // Tính hours của segment này (tính theo phút để chính xác)
            double minutes = Duration.between(cursor, segmentEnd).toSeconds() / 60.0;
            double hours = minutes / 60.0;
            BigDecimal hoursDecimal = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);

            if (hoursDecimal.compareTo(BigDecimal.ZERO) > 0) {
                WorkLog log = WorkLog.builder()
                        .issue(issue)
                        .user(user)
                        .hours(hoursDecimal)
                        .loggedAt(cursor.toLocalDate().atStartOfDay()) // lưu đầu ngày để group by ngày
                        .note(request.getNote())
                        .createdAt(LocalDateTime.now())
                        .build();
                logs.add(log);
            }

            // Sang ngày tiếp theo
            cursor = cursor.toLocalDate().plusDays(1).atStartOfDay();
        }

        workLogRepository.saveAll(logs);

        return logs.stream()
                .map(log -> toResponse(log, issue))
                .collect(Collectors.toList());
    }

    // Lấy tất cả work logs của một issue
    public List<WorkLogResponse> getByIssue(UUID issueId) {
        return workLogRepository.findAllByIssue_IdOrderByLoggedAtDesc(issueId)
                .stream()
                .map(log -> toResponse(log, log.getIssue()))
                .collect(Collectors.toList());
    }

    public WorkStatsResponse getWeeklyStats() {
        UUID userId = securityUtil.getCurrentUserId();
        LocalDate today = LocalDate.now();

        LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        return WorkStatsResponse.builder()
                .thisWeek(buildDailyHours(userId, thisWeekStart, thisWeekEnd))
                .lastWeek(buildDailyHours(userId, lastWeekStart, lastWeekEnd))
                .build();
    }

    public WorkStatsResponse getMonthlyStats() {
        UUID userId = securityUtil.getCurrentUserId();
        LocalDate today = LocalDate.now();

        LocalDate thisYearStart = today.withDayOfYear(1);
        LocalDate lastYearStart = thisYearStart.minusYears(1);

        return WorkStatsResponse.builder()
                .thisWeek(buildMonthlyHours(userId, thisYearStart, thisYearStart.plusYears(1).minusDays(1)))
                .lastWeek(buildMonthlyHours(userId, lastYearStart, thisYearStart.minusDays(1)))
                .build();
    }


    private static WorkLogResponse toResponse(WorkLog log, Issue issue) {
        return WorkLogResponse.builder()
                .id(log.getId())
                .issueId(issue.getId())
                .issueName(issue.getIssueName())
                .userProfileName(log.getUser().getProfileName())
                .userPicture(log.getUser().getPicture())
                .hours(log.getHours())
                .loggedAt(log.getLoggedAt())
                .note(log.getNote())
                .build();
    }

    private List<WorkStatsResponse.DailyHours> buildDailyHours(
            UUID userId, LocalDate from, LocalDate to) {

        List<Object[]> rows = workLogRepository.sumHoursByDay(
                userId,
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX)
        );
        Map<LocalDate, BigDecimal> map = rows.stream()
                .collect(Collectors.toMap(
                        r -> (LocalDate) r[0],
                        r -> (BigDecimal) r[1]
                ));

        List<WorkStatsResponse.DailyHours> result = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            result.add(WorkStatsResponse.DailyHours.builder()
                    .label(cur.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .hours(map.getOrDefault(cur, BigDecimal.ZERO))
                    .build());
            cur = cur.plusDays(1);
        }
        return result;
    }

    private List<WorkStatsResponse.DailyHours> buildMonthlyHours(
            UUID userId, LocalDate from, LocalDate to) {

        List<Object[]> rows = workLogRepository.sumHoursByDay(
                userId,
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX)
        );
        Map<Month, BigDecimal> map = new EnumMap<>(Month.class);
        for (Object[] r : rows) {
            LocalDate date = (LocalDate) r[0];
            BigDecimal h = (BigDecimal) r[1];
            map.merge(date.getMonth(), h, BigDecimal::add);
        }

        List<WorkStatsResponse.DailyHours> result = new ArrayList<>();
        for (Month month : Month.values()) {
            result.add(WorkStatsResponse.DailyHours.builder()
                    .label(month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .hours(map.getOrDefault(month, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }
}