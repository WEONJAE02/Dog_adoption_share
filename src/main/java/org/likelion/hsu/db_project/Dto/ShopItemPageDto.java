package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItemPageDto {
    private List<ShopItemResponseDto> items;
    private int page;         // 1부터 시작
    private int size;
    private long total;
    private int totalPages;
}
