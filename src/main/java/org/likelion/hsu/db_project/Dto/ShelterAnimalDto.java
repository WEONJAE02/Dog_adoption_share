package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

/**
 * 공공데이터(국가동물보호정보시스템)에서 가져온 보호소 유기견 1건
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterAnimalDto {
    private String desertionNo;    // 구조번호
    private String noticeNo;       // 공고번호
    private String kind;           // 품종
    private String color;
    private String age;            // 예: 2025년생
    private String weight;         // 예: 13kg
    private String sex;            // 수컷 / 암컷 / 미상
    private String neuter;         // 완료 / 안 함 / 미상
    private String state;          // 보호중, 종료(입양) 등
    private String happenDate;     // 접수일 (yyyy-MM-dd)
    private String happenPlace;    // 발견 장소
    private String noticeStart;
    private String noticeEnd;
    private String specialMark;    // 특징
    private List<String> images;
    private String careName;       // 보호소
    private String careTel;
    private String careAddr;
    private String orgName;        // 관할기관

    // 보호소가 등록한 입양 절차 (대부분 비어 있음)
    private String adoptionTitle;
    private String adoptionPeriod;
    private String adoptionCondition;
    private String adoptionText;
    private String adoptionImage;
}
