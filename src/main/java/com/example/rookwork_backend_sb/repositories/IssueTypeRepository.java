package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueType, UUID> {
    List<IssueType> findByProjectId(UUID projectId);
    Optional<IssueType> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);
    Optional<IssueType> findByIdAndProjectId(UUID id, UUID projectId);
}
