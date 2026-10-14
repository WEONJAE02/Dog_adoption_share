package org.likelion.hsu.db_project.Dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 필수 동의 항목 (회원가입, 소셜 로그인 첫 동의)
 */
@Getter
@Setter
public class ConsentRequestDto {
    private boolean ageOver14;
    private boolean agreeTerms;
    private boolean agreePrivacy;

    public boolean allAgreed() {
        return ageOver14 && agreeTerms && agreePrivacy;
    }
}
