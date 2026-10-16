package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MyPetRequestDto {
    private String name;
    private String species;
    private String breed;
    private String gender;
    private Boolean neutered;
    private Integer birthYear;
    private BigDecimal weightKg;
    private String notes;
    private boolean removeImage;   // 수정 시 기존 사진 삭제
}
