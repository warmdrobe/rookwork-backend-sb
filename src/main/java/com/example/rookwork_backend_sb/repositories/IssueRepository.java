package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Issue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {
    Optional<Issue> findById(UUID id);
    List<Issue> findAllByProjectId(UUID projectId);
    List<Issue> findAllByAssignedToId(UUID userId);
    Optional<Issue> findByIdAndProjectId(UUID id, UUID projectId);
}
