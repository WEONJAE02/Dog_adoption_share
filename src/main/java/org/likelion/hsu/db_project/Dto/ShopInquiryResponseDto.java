package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopInquiryResponseDto {
    private Long inquiryId;
    private Long itemId;
    private String itemTitle;
    private String itemImage;
    private int itemPrice;
    private String senderNickname;
    private String receiverNickname;
    private String message;
    private String contact;
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}
