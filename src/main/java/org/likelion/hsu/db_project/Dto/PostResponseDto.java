package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long postId;
    private Long userId;
    private String nickname;
    private String category;
    private String title;
    private int viewCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hidden;

    // 목록용
    private String thumbnail;

    // 상세용
    private String content;
    private List<String> imageNames;
    private List<CommentResponseDto> comments;
}
