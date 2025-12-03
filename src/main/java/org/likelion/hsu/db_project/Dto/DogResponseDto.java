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
    private Long dogId;
    private String name;
    private String breed;
    private String gender;
    private String foundPlace;
    private int age;
    private String status;
    private String memo;
    private List<String> imageNames;
    private LocalDateTime regDate;

    public static DogResponseDto from(Dog dog) {
        return DogResponseDto.builder()
                .dogId(dog.getDogId())
                .name(dog.getName())
                .breed(dog.getBreed())
                .foundPlace(dog.getDogDetail() != null ? dog.getDogDetail().getFoundPlace() : null)
                .age(dog.getAge())
                .status(dog.getStatus())
                .memo(dog.getDogDetail() != null ? dog.getDogDetail().getMemo() : null)
                .gender(dog.getGender())
                .imageNames(dog.getImages() != null
                        ? dog.getImages().stream().map(DogImage::getFileName).collect(Collectors.toList())
                        : null)
                .regDate(dog.getRegDate())
                .build();
    }
}
