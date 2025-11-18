package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DogImage {
    private Long id;              // PK
    private Long dogId;           // ✅ FK (dog 테이블 참조)
    private String fileName;
    private String filePath;
    private LocalDateTime uploadDate;
}
