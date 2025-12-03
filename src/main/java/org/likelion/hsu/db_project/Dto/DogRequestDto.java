package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DogRequestDto {
    private String name;
    private String breed;
    private String gender;
    private String foundPlace;
    private int age;
    private String status;
    private String memo;
}
