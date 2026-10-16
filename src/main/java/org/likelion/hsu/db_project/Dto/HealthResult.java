package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

/**
 * 건강체크 분석 결과. 샘플 분석기와 AI 분석기가 같은 형식으로 돌려준다.
 * (health_check.result_json 에 그대로 저장)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResult {

    public static final String EMERGENCY = "EMERGENCY";   // 바로 병원
    public static final String SOON = "SOON";             // 2~3일 안에 병원
    public static final String MONITOR = "MONITOR";       // 집에서 지켜보기

    private String urgency;
    private String urgencyReason;
    private List<Finding> findings;
    private String summary;
    private List<String> possibleCauses;
    private String specialty;            // 안과 / 피부과 / 치과 / 정형외과 / 내과 / 신경과 / 일반
    private List<String> homeCare;
    private List<String> questionsForVet;
    private String imageQuality;         // OK / POOR
    private String retakeTip;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private int frame;               // 1부터 시작하는 프레임 번호
        private String observation;
        private String confidence;       // low / medium / high
    }
}
