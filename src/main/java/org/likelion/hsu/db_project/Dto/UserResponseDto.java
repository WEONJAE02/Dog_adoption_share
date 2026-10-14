package org.likelion.hsu.db_project.Dto;

import lombok.*;
import org.likelion.hsu.db_project.Security.LoginUser;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long userId;
    private String provider;
    private String email;
    private String nickname;

    public static UserResponseDto from(LoginUser user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
