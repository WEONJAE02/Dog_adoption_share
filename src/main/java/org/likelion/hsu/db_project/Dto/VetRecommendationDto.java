package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetRecommendationDto {
    private Long vetId;
    private String name;
    private String clinicName;
    private String sido;
    private String sigungu;
    private String specialty;
    private List<String> subSpecialties;
    private int careerYears;
    private int casesCount;
    private BigDecimal rating;
    private int reviewCount;
    private boolean open24h;
    private String intro;
    private boolean sample;

    private int score;               // 0 ~ 100
    private List<String> reasons;    // 왜 추천했는지 (화면에 그대로 보여준다)
}
