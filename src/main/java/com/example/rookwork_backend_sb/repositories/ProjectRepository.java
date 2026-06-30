package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    //find project
    Optional<Project> findById(UUID id);

    @Query("SELECT p FROM Project p ORDER BY SIZE(p.issues) DESC")
    List<Project> findTopActiveProjects(Pageable pageable);
}
