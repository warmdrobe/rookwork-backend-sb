package com.example.rookwork_backend_sb.dtos.comments;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về thông tin tổng hợp của một loại biểu cảm trên một bình luận.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionResponse {

    /** Loại emoji, ví dụ: "👍", "❤️", "😂", "🎉", "😢", "😮" */
    private String reactionType;

    /** Số lượng người dùng đã thả biểu cảm này. */
    private int count;

    /** Danh sách thông tin cơ bản của các user đã thả biểu cảm (để hiển thị tooltip). */
    private List<UserSummary> users;
}
