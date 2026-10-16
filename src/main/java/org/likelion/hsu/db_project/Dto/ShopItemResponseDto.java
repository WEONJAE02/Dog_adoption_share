package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItemResponseDto {
    private Long itemId;
    private Long userId;
    private String nickname;
    private String category;
    private String title;
    private int price;
    private String tradeArea;
    private String status;
    private int viewCount;
    private int likeCount;
    private boolean liked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hidden;

    // 목록용
    private String thumbnail;

    // 상세용
    private String description;
    private List<String> imageNames;
}
