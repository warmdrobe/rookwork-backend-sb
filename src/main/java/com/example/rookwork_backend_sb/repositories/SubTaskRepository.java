package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, UUID> {
    List<SubTask> findByIssueId(UUID issueId);
    Optional<SubTask> findByIdAndIssueId(UUID id, UUID issueId);
}