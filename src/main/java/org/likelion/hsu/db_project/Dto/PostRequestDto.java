package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostRequestDto {
    private String title;
    private String content;
    private String category;
    private List<String> keepImages;   // 수정 시 유지할 기존 이미지 파일명
}
