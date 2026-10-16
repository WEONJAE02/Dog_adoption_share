package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheckResponseDto {
    private Long checkId;
    private Long petId;
    private String petName;
    private String petSpecies;
    private String bodyPart;
    private String source;
    private List<String> symptoms;
    private String memo;
    private String urgency;
    private String specialty;
    private String summary;
    private String analyzer;          // sample / 모델 이름
    private LocalDateTime createdAt;

    // 목록용
    private String thumbnail;

    // 상세용
    private HealthResult result;
    private List<String> frames;
}
