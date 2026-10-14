package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {
    private Long inquiryId;
    private Long dogId;
    private Long senderId;        // 문의한 사람
    private Long receiverId;      // 유기견 등록자
    private String message;
    private String contact;       // 문의한 사람이 남긴 연락처 (등록자에게만 보임)
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;

    // 조회 시 함께 가져오는 값
    private String dogName;
    private String dogImage;
    private String senderNickname;
    private String receiverNickname;
}
