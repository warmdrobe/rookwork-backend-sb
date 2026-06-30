package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByIssueId(UUID issueId);

    @Query("SELECT f FROM File f JOIN f.issue i JOIN i.project p JOIN p.projectMembers pm WHERE pm.user.id = :userId AND " +
           "LOWER(f.originalName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<File> searchFilesForUser(@Param("userId") UUID userId, @Param("query") String query);
}
