package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByIssueId(UUID issueId);
}
