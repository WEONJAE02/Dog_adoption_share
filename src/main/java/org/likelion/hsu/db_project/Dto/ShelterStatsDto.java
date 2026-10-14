package org.likelion.hsu.db_project.Dto;

import lombok.*;

/**
 * 최근 N일 동안 구조된 유기견의 처리 현황
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterStatsDto {
    private String from;            // 집계 시작일 (yyyy-MM-dd)
    private String to;              // 집계 종료일 (yyyy-MM-dd)
    private int days;
    private long rescued;           // 구조된 수
    private long adopted;
    private long returned;          // 보호자에게 반환
    private long euthanized;
    private long protecting;        // 공고중 + 보호중 (아직 처리되지 않은 수)
    private long closed;            // 처리가 끝난 수 (입양, 반환, 안락사, 자연사 등)
    private double adoptionRate;    // 입양 / 처리 완료 (%)
    private double euthanasiaRate;  // 안락사 / 처리 완료 (%)
}
