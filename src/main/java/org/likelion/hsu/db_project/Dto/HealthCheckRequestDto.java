package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HealthCheckRequestDto {
    private Long petId;
    private String bodyPart;        // 눈 / 피부 / 귀 / 치아·입 / 걸음걸이 / 기타
    private String source;          // video / photo
    private List<String> symptoms;  // 체크리스트에서 고른 증상
    private String memo;
}
