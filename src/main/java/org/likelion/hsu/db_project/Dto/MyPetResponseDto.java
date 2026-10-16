package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPetResponseDto {
    private Long petId;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private Boolean neutered;
    private Integer birthYear;
    private BigDecimal weightKg;
    private String notes;
    private String imageName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
