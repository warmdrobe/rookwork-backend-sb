package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    //find project
    Optional<Project> findById(UUID id);

    @Query("SELECT p FROM Project p JOIN p.projectMembers pm WHERE pm.user.id = :userId AND " +
           "(LOWER(p.projectName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Project> searchProjectsForUser(@Param("userId") UUID userId, @Param("query") String query);
}
