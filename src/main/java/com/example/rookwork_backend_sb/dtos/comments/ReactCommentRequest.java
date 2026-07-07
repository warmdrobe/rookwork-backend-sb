package com.example.rookwork_backend_sb.dtos.comments;

import lombok.Data;

/**
 * Payload yêu cầu thả / đổi / gỡ biểu cảm trên một bình luận.
 */
@Data
public class ReactCommentRequest {

    /**
     * Loại emoji muốn thả, ví dụ: "👍", "❤️", "😂", "🎉", "😢", "😮".
     * Giá trị hợp lệ được kiểm tra phía service.
     */
    private String reactionType;
}
