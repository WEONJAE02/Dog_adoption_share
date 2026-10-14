package org.likelion.hsu.db_project.Entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long userId;          // PK
    private String provider;      // local / kakao / naver
    private String providerId;    // local: 이메일, 소셜: 제공자 측 회원 식별자
    private String email;
    private String password;      // local만 사용 (BCrypt 해시)
    private String nickname;
    private LocalDateTime createdAt;
}
