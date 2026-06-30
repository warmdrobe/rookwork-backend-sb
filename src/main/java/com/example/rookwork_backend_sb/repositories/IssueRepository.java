package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Issue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.rookwork_backend_sb.entities.Status;
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
    long countByProjectIdAndStatus(UUID projectId, Status status);

    @Query("SELECT MAX(i.deadline) FROM Issue i WHERE i.project.id = :projectId")
    java.time.Instant findMaxDeadlineByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT i FROM Issue i JOIN i.project p JOIN p.projectMembers pm WHERE pm.user.id = :userId AND " +
           "(LOWER(i.issueName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Issue> searchIssuesForUser(@Param("userId") UUID userId, @Param("query") String query);
}
