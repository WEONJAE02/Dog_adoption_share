package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long commentId;
    private Long userId;
    private String nickname;
    private String content;
    private boolean deleted;
    private boolean hidden;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentResponseDto> replies = new ArrayList<>();
}
