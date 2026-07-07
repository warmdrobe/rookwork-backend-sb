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

/**
 * Service class handling logging work hours on issues, segmenting work spans by day, and calculating stats.
 */
@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final S3Service s3Service;
    
    /**
     * Logs work hours. If the work span crosses day boundaries (UTC), it automatically splits into daily segments.
     *
     * @param request the log work details, including issue ID and duration span
     * @return a list of created WorkLogResponse DTOs
     * @throws ResourceNotFoundException if user or issue is not found
     * @throws BadRequestException if timestamps are missing or endAt is before startAt
     */
    public List<WorkLogResponse> logWork(LogWorkRequest request) {
        Issue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        UUID currentUserId = securityUtil.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAssignee = issue.getAssignees().stream()
                .anyMatch(u -> u.getId().equals(currentUserId));
        if (!isAssignee) {
            throw new BadRequestException("You can only log work on an issue assigned to you");
        }

        // Validate time inputs
        if (request.getStartAt() == null || request.getEndAt() == null)
            throw new BadRequestException("startAt and endAt are required");
        if (!request.getEndAt().isAfter(request.getStartAt()))
            throw new BadRequestException("endAt must be after startAt");

        // Check for overlapping work logs for the same user within the same issue
        boolean hasOverlap = workLogRepository.existsByUser_IdAndIssue_IdAndStartAtBeforeAndEndAtAfter(
                currentUserId, issue.getId(), request.getEndAt(), request.getStartAt()
        );
        if (hasOverlap) {
            throw new BadRequestException("You have already logged work during this time period on this issue");
        }

        ZoneId zoneId = ZoneOffset.UTC;
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try {
                zoneId = ZoneId.of(request.getTimezone());
            } catch (Exception e) {
                // fallback to UTC
            }
        }

        ZonedDateTime start = request.getStartAt().atZone(zoneId);
        ZonedDateTime end = request.getEndAt().atZone(zoneId);

        // Restrict logging in the past (must be today or later in the user's local timezone)
        ZonedDateTime todayStart = ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId);
        if (start.isBefore(todayStart)) {
            throw new BadRequestException("You cannot log work for past days");
        }

        // Split the work duration into segments by day
        List<WorkLog> logs = new ArrayList<>();
        ZonedDateTime cursor = start;

        while (cursor.isBefore(end)) {
            // Get end of the current day in local timezone (11:59 PM)
            ZonedDateTime dayEnd = cursor.toLocalDate().atTime(23, 59, 0).atZone(zoneId);
            // End the current segment at the earlier of end of day or total end time
            ZonedDateTime segmentEnd = dayEnd.isBefore(end) ? dayEnd : end;

            // Calculate duration in hours
            double minutes = Duration.between(cursor, segmentEnd).toSeconds() / 60.0;
            double hours = minutes / 60.0;
            BigDecimal hoursDecimal = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);

            if (hoursDecimal.compareTo(BigDecimal.ZERO) > 0) {
                WorkLog log = WorkLog.builder()
                        .issue(issue)
                        .user(currentUser)
                        .hours(hoursDecimal)
                        .loggedAt(cursor.toLocalDate().atStartOfDay(zoneId).toInstant()) // Store start of day in local zone converted to Instant
                        .startAt(cursor.toInstant())
                        .endAt(segmentEnd.toInstant())
                        .note(request.getNote())
                        .createdAt(Instant.now())
                        .build();
                logs.add(log);
            }

            // Advance cursor to the next day in local timezone
            cursor = cursor.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        }

        workLogRepository.saveAll(logs);

        return logs.stream()
                .map(log -> toResponse(log, issue))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all work logs for a specific issue, ordered by logged date descending.
     *
     * @param issueId the unique identifier of the issue
     * @return a list of WorkLogResponse DTOs
     */
    public List<WorkLogResponse> getByIssue(UUID issueId) {
        return workLogRepository.findAllByIssue_IdOrderByLoggedAtDesc(issueId)
                .stream()
                .map(log -> toResponse(log, log.getIssue()))
                .collect(Collectors.toList());
    }

    /**
     * Aggregates weekly work stats (this week vs last week) for the current user.
     *
     * @return a WorkStatsResponse containing daily breakdown
     */
    public WorkStatsResponse getWeeklyStats(String timezone) {
        UUID userId = securityUtil.getCurrentUserId();

        ZoneId zoneId = ZoneId.systemDefault();
        if (timezone != null && !timezone.isEmpty()) {
            try {
                zoneId = ZoneId.of(timezone);
            } catch (Exception e) {
                // fallback to systemDefault
            }
        }

        LocalDate today = LocalDate.now(zoneId);

        LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        return WorkStatsResponse.builder()
                .thisWeek(buildDailyHours(userId, thisWeekStart, thisWeekEnd, zoneId))
                .lastWeek(buildDailyHours(userId, lastWeekStart, lastWeekEnd, zoneId))
                .build();
    }

    public WorkStatsResponse getMonthlyStats(String timezone) {
        UUID userId = securityUtil.getCurrentUserId();

        ZoneId zoneId = ZoneId.systemDefault();
        if (timezone != null && !timezone.isEmpty()) {
            try {
                zoneId = ZoneId.of(timezone);
            } catch (Exception e) {
                // fallback to systemDefault
            }
        }

        LocalDate today = LocalDate.now(zoneId);

        LocalDate thisYearStart = today.withDayOfYear(1);
        LocalDate lastYearStart = thisYearStart.minusYears(1);

        return WorkStatsResponse.builder()
                .thisWeek(buildMonthlyHours(userId, thisYearStart, thisYearStart.plusYears(1).minusDays(1), zoneId))
                .lastWeek(buildMonthlyHours(userId, lastYearStart, thisYearStart.minusDays(1), zoneId))
                .build();
    }


    private WorkLogResponse toResponse(WorkLog log, Issue issue) {
        Instant startAt = log.getStartAt() != null ? log.getStartAt() : log.getLoggedAt();
        Instant endAt = log.getEndAt() != null ? log.getEndAt() : log.getLoggedAt().plus(Duration.ofMinutes((long)(log.getHours().doubleValue() * 60)));
        Instant createdAt = log.getCreatedAt() != null ? log.getCreatedAt() : log.getLoggedAt();

        return WorkLogResponse.builder()
                .id(log.getId())
                .issueId(issue.getId())
                .issueName(issue.getIssueName())
                .userProfileName(log.getUser().getProfileName())
                .userPicture(s3Service.getAvatarUrl(log.getUser().getPicture()))
                .hours(log.getHours())
                .loggedAt(log.getLoggedAt())
                .startAt(startAt)
                .endAt(endAt)
                .createdAt(createdAt)
                .note(log.getNote())
                .build();
    }

    private List<WorkStatsResponse.DailyHours> buildDailyHours(
            UUID userId, LocalDate from, LocalDate to, ZoneId zoneId) {

        List<WorkLog> logs = workLogRepository.findAllByUser_IdAndLoggedAtBetween(
                userId,
                from.atStartOfDay(zoneId).toInstant(),
                to.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
        );
        Map<LocalDate, BigDecimal> map = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getLoggedAt().atZone(zoneId).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, WorkLog::getHours, BigDecimal::add)
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
            UUID userId, LocalDate from, LocalDate to, ZoneId zoneId) {

        List<WorkLog> logs = workLogRepository.findAllByUser_IdAndLoggedAtBetween(
                userId,
                from.atStartOfDay(zoneId).toInstant(),
                to.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
        );
        Map<Month, BigDecimal> map = new EnumMap<>(Month.class);
        for (WorkLog log : logs) {
            LocalDate date = log.getLoggedAt().atZone(zoneId).toLocalDate();
            map.merge(date.getMonth(), log.getHours(), BigDecimal::add);
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