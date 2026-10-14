package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    private Long commentId;
    private Long postId;
    private Long userId;          // 작성자
    private Long parentId;        // 대댓글이면 부모 댓글 ID
    private String content;
    private boolean deleted;
    private LocalDateTime createdAt;

    private String nickname;      // 조회 시 함께 가져오는 값
}
