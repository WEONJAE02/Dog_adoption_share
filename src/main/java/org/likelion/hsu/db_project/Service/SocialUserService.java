package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Entity.User;
import org.likelion.hsu.db_project.Repository.UserRepository;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 카카오/네이버 로그인 후 사용자 정보를 받아 users 테이블과 연결한다.
 * 처음 로그인한 사용자는 자동으로 가입 처리한다.
 */
@Service
@RequiredArgsConstructor
public class SocialUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        User profile = toProfile(provider, oAuth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(provider, profile.getProviderId())
                .orElseGet(() -> register(profile));

        return new LoginUser(user);
    }

    private User register(User profile) {
        try {
            profile.setUserId(userRepository.save(profile));
            return profile;
        } catch (DuplicateKeyException e) {
            // 같은 계정으로 동시에 첫 로그인한 경우
            return userRepository.findByProviderAndProviderId(profile.getProvider(), profile.getProviderId())
                    .orElseThrow(() -> e);
        }
    }

    // ✅ 제공자별 응답 형식에서 식별자/닉네임/이메일 추출
    static User toProfile(String provider, Map<String, Object> attributes) {
        Object id;
        String nickname;
        String email;

        if ("kakao".equals(provider)) {
            // { id, kakao_account: { email, profile: { nickname } } }
            Map<String, Object> account = asMap(attributes.get("kakao_account"));
            id = attributes.get("id");
            nickname = asString(asMap(account.get("profile")).get("nickname"));
            email = asString(account.get("email"));
        } else if ("naver".equals(provider)) {
            // { resultcode, message, response: { id, nickname, name, email } }
            Map<String, Object> response = asMap(attributes.get("response"));
            id = response.get("id");
            nickname = asString(response.get("nickname"));
            if (nickname == null) nickname = asString(response.get("name"));
            email = asString(response.get("email"));
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }

        if (id == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"));
        }
        if (nickname == null) {
            nickname = "kakao".equals(provider) ? "카카오 사용자" : "네이버 사용자";
        }

        return User.builder()
                .provider(provider)
                .providerId(String.valueOf(id))
                .email(email)
                .nickname(nickname.length() > 50 ? nickname.substring(0, 50) : nickname)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static String asString(Object value) {
        return value instanceof String s && !s.isBlank() ? s.trim() : null;
    }
}
