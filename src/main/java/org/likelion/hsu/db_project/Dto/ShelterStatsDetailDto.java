package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

/**
 * 전국 유기동물 상세 통계 (기간 + 축종별)
 * 입양률·안락사율·반환율은 처리가 끝난 건(closed) 기준
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterStatsDetailDto {
    private String kind;            // dog / cat / all
    private String from;
    private String to;
    private int days;

    private long rescued;           // 구조된 수
    private long closed;            // 처리가 끝난 수
    private long protecting;        // 공고·보호 중
    private double adoptionRate;
    private double returnRate;
    private double euthanasiaRate;

    private List<Count> outcomes;   // 처리 결과 (입양, 반환, 기증, 방사, 자연사, 안락사, 보호·공고 중)
    private List<Count> daily;      // 일별 구조 수 (yyyy-MM-dd)
    private List<Group> regions;    // 시도별
    private List<Count> breeds;     // 많이 구조된 품종 상위 10
    private List<Group> ages;       // 나이대별
    private List<Group> sizes;      // 체중별 (개만)
    private List<Count> species;    // 축종별 구조 수 (all 일 때만)
    private List<Count> sex;
    private List<Count> neuter;

    @Getter
    @AllArgsConstructor
    public static class Count {
        private String label;
        private long count;
    }

    @Getter
    @AllArgsConstructor
    public static class Group {
        private String label;
        private long rescued;
        private long closed;
        private long adopted;
        private long euthanized;
        private double adoptionRate;
        private double euthanasiaRate;
    }
}
