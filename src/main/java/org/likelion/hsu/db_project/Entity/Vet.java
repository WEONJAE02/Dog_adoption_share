package org.likelion.hsu.db_project.Entity;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** 주치의 추천용 샘플 수의사 (가상 인물·가상 병원) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vet {
    private Long vetId;
    private String name;
    private String clinicName;
    private String sido;
    private String sigungu;
    private String specialty;            // 주 진료 분야
    private List<String> subSpecialties; // 함께 보는 분야
    private int careerYears;
    private int casesCount;              // 주 분야 진료 건수
    private BigDecimal rating;           // 0.0 ~ 5.0
    private int reviewCount;
    private boolean open24h;
    private String intro;
    private boolean sample;
}
