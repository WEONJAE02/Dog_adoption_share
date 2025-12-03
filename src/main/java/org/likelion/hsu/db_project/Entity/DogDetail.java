package org.likelion.hsu.db_project.Entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DogDetail {
    private Long id;      // PK
    private Long dogId;   // FK (dog 테이블 참조)
    private String foundPlace;
    private String memo;
}
