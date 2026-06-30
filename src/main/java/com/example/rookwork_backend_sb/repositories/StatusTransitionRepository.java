package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.StatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusTransitionRepository extends JpaRepository<StatusTransition, UUID> {

    List<StatusTransition> findAllByProjectId(UUID projectId);

    boolean existsByProjectIdAndFromStatusIdAndToStatusId(UUID projectId, UUID fromStatusId, UUID toStatusId);

    Optional<StatusTransition> findByProjectIdAndFromStatusIdAndToStatusId(UUID projectId, UUID fromStatusId, UUID toStatusId);

    void deleteAllByProjectId(UUID projectId);

    long countByProjectId(UUID projectId);
}
