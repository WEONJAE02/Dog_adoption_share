package org.likelion.hsu.db_project.Entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheck {
    private Long checkId;
    private Long petId;
    private Long userId;
    private String bodyPart;
    private String source;
    private String symptoms;      // 쉼표로 구분
    private String memo;
    private String urgency;
    private String specialty;
    private String summary;
    private String resultJson;
    private String analyzer;
    private LocalDateTime createdAt;

    // 조회 시 함께 가져오는 값
    private String petName;
    private String petSpecies;
    private String thumbnail;
}
