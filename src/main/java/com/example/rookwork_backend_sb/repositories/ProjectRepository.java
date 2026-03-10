package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    //find project
    Optional<Project> findById(UUID id);
}
