package com.example.rookwork_backend_sb.repositories;

import com.example.rookwork_backend_sb.entities.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {

    /** Tìm reaction của một user cụ thể trên một comment cụ thể. */
    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);

    /** Lấy tất cả reactions của một comment. */
    List<CommentReaction> findByCommentId(UUID commentId);
}
