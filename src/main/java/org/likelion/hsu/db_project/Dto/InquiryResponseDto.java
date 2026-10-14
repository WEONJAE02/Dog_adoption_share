package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponseDto {
    private Long inquiryId;
    private Long dogId;
    private String dogName;
    private String dogImage;
    private String senderNickname;
    private String receiverNickname;
    private String message;
    private String contact;
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}
