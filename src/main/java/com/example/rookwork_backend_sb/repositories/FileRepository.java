package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByIssueId(UUID issueId);

    @Query("SELECT f.issue.project.id, COUNT(f), COALESCE(SUM(f.sizeBytes), 0) FROM File f WHERE f.issue.project IS NOT NULL GROUP BY f.issue.project.id")
    List<Object[]> getFileStatsByProject();

    @Query("SELECT f.user.id, COUNT(f), COALESCE(SUM(f.sizeBytes), 0) FROM File f GROUP BY f.user.id")
    List<Object[]> getFileStatsByUser();
}
