package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, UUID> {

    List<ProjectStatus> findAllByProjectIdOrderByPositionAsc(UUID projectId);

    long countByProjectId(UUID projectId);

    Optional<ProjectStatus> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("SELECT MAX(ps.position) FROM ProjectStatus ps WHERE ps.project.id = :projectId")
    Optional<Integer> findMaxPositionByProjectId(@Param("projectId") UUID projectId);
}
