package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findById (UUID id);
    List<Comment> findByIssueId(UUID issueId);
    List<Comment> findByIssueProjectId(UUID projectId);
    List<Comment> findByIssueIdAndParentCommentIsNull(UUID issueId);

    @Query("SELECT c.issue.project.id, COUNT(c) FROM Comment c WHERE c.issue.project IS NOT NULL GROUP BY c.issue.project.id")
    List<Object[]> getCommentCountByProject();
}
