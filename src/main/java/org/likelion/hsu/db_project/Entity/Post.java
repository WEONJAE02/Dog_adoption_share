package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    private Long postId;
    private Long userId;          // 작성자
    private String category;
    private String title;
    private String content;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hidden;       // 신고 누적으로 숨김

    // 조회 시 함께 가져오는 값 (JPA가 아니므로 단순 필드로 유지)
    private String nickname;
    private int commentCount;
    private String thumbnail;
}
