package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShopItemRequestDto {
    private String category;
    private String title;
    private Integer price;             // 0 이면 나눔
    private String description;
    private String tradeArea;
    private String status;             // 상태 변경 시 사용
    private List<String> keepImages;   // 수정 시 유지할 기존 이미지 파일명
}
