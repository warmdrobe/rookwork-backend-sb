package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Issue;
import com.example.rookwork_backend_sb.entities.StatusCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {
    Optional<Issue> findById(UUID id);
    List<Issue> findAllByProjectId(UUID projectId);
    Optional<Issue> findByIdAndProjectId(UUID id, UUID projectId);
    long countByIssueTypeId(UUID issueTypeId);

    @Query("SELECT i FROM Issue i JOIN i.assignees u WHERE u.id = :userId")
    List<Issue> findAllByAssigneeId(@Param("userId") UUID userId);

    //process
    long countByProjectId(UUID projectId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.id = :projectId AND i.status.statusCategory = :category")
    long countByProjectIdAndStatusCategory(@Param("projectId") UUID projectId, @Param("category") StatusCategory category);

    @Query("SELECT MAX(i.deadline) FROM Issue i WHERE i.project.id = :projectId")
    java.time.Instant findMaxDeadlineByProjectId(@Param("projectId") UUID projectId);
}
