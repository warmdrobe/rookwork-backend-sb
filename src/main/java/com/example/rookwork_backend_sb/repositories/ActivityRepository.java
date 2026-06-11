package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
  List<Activity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

  List<Activity> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

  @Query("SELECT a FROM Activity a WHERE a.project.id = :projectId AND a.entityId = :issueId AND a.entityType = 'ISSUE' ORDER BY a.createdAt DESC")
  List<Activity> findIssueActivities(
      @Param("projectId") UUID projectId,
      @Param("issueId") UUID issueId,
      Pageable pageable);

}
