package org.likelion.hsu.db_project.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 요청에 없는/불필요한 필드(예: gender) 무시
public class DogRequestDto {
    private String name;
    private String breed;
    private String foundPlace;
    private Integer age;
    private String status;
    private String memo;
    private String gender;
}
