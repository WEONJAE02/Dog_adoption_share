package org.likelion.hsu.db_project.Dto;

import lombok.*;
import org.likelion.hsu.db_project.Entity.Dog;
import org.likelion.hsu.db_project.Entity.DogImage;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DogResponseDto {
    private Long id;
    private String name;
    private String breed;
    private String foundPlace;
    private Integer age;
    private String status;
    private String memo;
    private String imageUrl;
    private LocalDateTime regDate;
    private LocalDateTime updatedAt;
    private String gender;

    public static DogResponseDto from(Dog d){
        return DogResponseDto.builder()
                .id(d.getId())
                .name(d.getName())
                .breed(d.getBreed())
                .foundPlace(d.getFoundPlace())
                .age(d.getAge())
                .status(d.getStatus())
                .memo(d.getMemo())
                .imageUrl(d.getImageUrl())
                .regDate(d.getRegDate())
                .updatedAt(d.getUpdatedAt())
                .gender(d.getGender())
                .build();
    }
}
