package org.likelion.hsu.db_project.Entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPet {
    private Long petId;
    private Long userId;          // 보호자
    private String name;
    private String species;       // 개 / 고양이
    private String breed;
    private String gender;        // 수컷 / 암컷 (모르면 null)
    private Boolean neutered;     // 중성화 여부 (모르면 null)
    private Integer birthYear;
    private BigDecimal weightKg;
    private String notes;         // 지병, 알러지, 복용 중인 약 등
    private String imageName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
