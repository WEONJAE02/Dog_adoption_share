package org.likelion.hsu.db_project.Service;

import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Entity.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SocialUserServiceTest {

    @Test
    void 카카오_응답에서_프로필을_추출한다() {
        Map<String, Object> attributes = Map.of(
                "id", 4012345678L,
                "kakao_account", Map.of(
                        "email", "dog@kakao.com",
                        "profile", Map.of("nickname", "초코맘")));

        User user = SocialUserService.toProfile("kakao", attributes);

        assertEquals("kakao", user.getProvider());
        assertEquals("4012345678", user.getProviderId());
        assertEquals("초코맘", user.getNickname());
        assertEquals("dog@kakao.com", user.getEmail());
    }

    @Test
    void 카카오_동의항목이_없으면_기본_닉네임을_쓴다() {
        User user = SocialUserService.toProfile("kakao", Map.of("id", 1L));

        assertEquals("카카오 사용자", user.getNickname());
        assertNull(user.getEmail());
    }

    @Test
    void 네이버_응답에서_프로필을_추출한다() {
        Map<String, Object> attributes = Map.of(
                "resultcode", "00",
                "message", "success",
                "response", Map.of("id", "abcDEF123", "name", "홍길동", "email", "dog@naver.com"));

        User user = SocialUserService.toProfile("naver", attributes);

        assertEquals("naver", user.getProvider());
        assertEquals("abcDEF123", user.getProviderId());
        assertEquals("홍길동", user.getNickname());
        assertEquals("dog@naver.com", user.getEmail());
    }

    @Test
    void 식별자가_없으면_로그인을_거부한다() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> SocialUserService.toProfile("naver", Map.of("resultcode", "024")));
    }
}
