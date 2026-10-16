package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopItem {
    private Long itemId;
    private Long userId;          // 판매자
    private String category;
    private String title;
    private int price;            // 0 이면 나눔
    private String description;
    private String tradeArea;     // 거래 희망 지역
    private String status;        // 판매중 / 예약중 / 거래완료
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hidden;       // 신고 누적으로 숨김

    // 조회 시 함께 가져오는 값
    private String nickname;
    private String thumbnail;
    private int likeCount;
    private boolean liked;        // 보는 사람이 관심 등록했는지
}
