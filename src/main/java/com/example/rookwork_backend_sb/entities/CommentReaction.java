package com.example.rookwork_backend_sb.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity biểu diễn một biểu cảm (reaction) của người dùng trên một bình luận.
 * Mỗi người dùng chỉ có tối đa một loại reaction trên một bình luận (được đảm bảo bởi unique constraint DB).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(
    name = "comment_reactions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_comment_reactions_comment_user",
        columnNames = {"comment_id", "user_id"}
    )
)
public class CommentReaction {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Loại emoji, ví dụ: "👍", "❤️", "😂", "🎉", "😢", "😮" */
    @Column(name = "reaction_type", nullable = false, length = 50)
    private String reactionType;

    @Column(name = "created_at")
    private Instant createdAt;
}
