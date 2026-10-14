package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto extends ConsentRequestDto {
    private String email;
    private String password;
    private String nickname;
}
