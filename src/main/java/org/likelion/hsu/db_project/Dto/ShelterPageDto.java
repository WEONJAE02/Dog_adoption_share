package org.likelion.hsu.db_project.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterPageDto {
    private List<ShelterAnimalDto> items;
    private int page;
    private int size;
    private long total;
}
