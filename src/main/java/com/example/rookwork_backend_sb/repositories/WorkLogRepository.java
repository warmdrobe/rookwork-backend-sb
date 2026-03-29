package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {

    // Lấy tất cả logs của user trong khoảng thời gian
    List<WorkLog> findAllByUser_IdAndLoggedAtBetween(
            UUID userId, LocalDateTime from, LocalDateTime to
    );

    // Lấy tất cả logs của một issue (để hiển thị trong TaskPanel)
    List<WorkLog> findAllByIssue_IdOrderByLoggedAtDesc(UUID issueId);

    // Tổng giờ theo ngày của user
    @Query("""
        SELECT CAST(w.loggedAt AS LocalDate), SUM(w.hours)
        FROM WorkLog w
        WHERE w.user.id = :userId
        AND w.loggedAt BETWEEN :from AND :to
        GROUP BY CAST(w.loggedAt AS LocalDate)
        ORDER BY CAST(w.loggedAt AS LocalDate)
    """)
    List<Object[]> sumHoursByDay(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}