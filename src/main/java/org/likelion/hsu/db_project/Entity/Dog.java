package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dog {
    private Long dogId;
    private String name;
    private String breed;
    private String gender;
    private int age;
    private String status;
    private String imageName;
    private LocalDateTime regDate;

    // ✅ 연관 데이터 포함 (JPA가 아니므로 단순 참조만 유지)
    private DogDetail dogDetail;

    @Builder.Default
    private List<DogImage> images = new ArrayList<>();
}
