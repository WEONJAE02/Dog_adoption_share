package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryRequestDto {
    private String message;
    private String contact;
    private String reply;      // 답변 등록 시 사용
}
