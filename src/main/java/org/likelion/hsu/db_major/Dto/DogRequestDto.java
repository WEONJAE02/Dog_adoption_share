package org.likelion.hsu.db_major.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DogRequestDto {
    private String name;
    private String breed;
    private String foundPlace;
    private int age;
    private String status;
    private String memo;
}
