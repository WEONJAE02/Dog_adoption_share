package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostPageDto {
    private List<PostResponseDto> items;
    private int page;         // 1부터 시작
    private int size;
    private long total;
    private int totalPages;
}
