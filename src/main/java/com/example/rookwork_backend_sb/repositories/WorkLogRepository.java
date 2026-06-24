package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {

    // Lấy tất cả logs của user trong khoảng thời gian
    List<WorkLog> findAllByUser_IdAndLoggedAtBetween(
            UUID userId, Instant from, Instant to
    );

    // Lấy tất cả logs của một issue (để hiển thị trong TaskPanel)
    List<WorkLog> findAllByIssue_IdOrderByLoggedAtDesc(UUID issueId);

    // Kiểm tra trùng lặp khoảng thời gian làm việc của cùng một user trong cùng một issue
    boolean existsByUser_IdAndIssue_IdAndStartAtBeforeAndEndAtAfter(
            UUID userId, UUID issueId, Instant endAt, Instant startAt
    );

}