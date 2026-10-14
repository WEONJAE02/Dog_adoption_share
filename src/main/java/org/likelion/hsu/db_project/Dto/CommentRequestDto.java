package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {
    private String content;
    private Long parentId;   // 대댓글이면 부모 댓글 ID
}
