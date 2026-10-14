package org.likelion.hsu.db_project.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 카카오/네이버 로그인 설정.
 * client-id와 client-secret이 모두 있는 제공자만 활성화된다. (키가 없어도 서버는 정상 기동)
 */
@Component
public class SocialLoginProperties {

    private static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Value("${oauth.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.naver.client-id:}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret:}")
    private String naverClientSecret;

    public List<ClientRegistration> registrations() {
        List<ClientRegistration> list = new ArrayList<>();

        // scope는 지정하지 않는다 → 각 개발자 콘솔에서 설정한 동의 항목을 그대로 따른다
        if (!kakaoClientId.isBlank() && !kakaoClientSecret.isBlank()) {
            list.add(ClientRegistration.withRegistrationId("kakao")
                    .clientName("Kakao")
                    .clientId(kakaoClientId)
                    .clientSecret(kakaoClientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(REDIRECT_URI)
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .tokenUri("https://kauth.kakao.com/oauth/token")
                    .userInfoUri("https://kapi.kakao.com/v2/user/me")
                    .userNameAttributeName("id")
                    .build());
        }

        if (!naverClientId.isBlank() && !naverClientSecret.isBlank()) {
            list.add(ClientRegistration.withRegistrationId("naver")
                    .clientName("Naver")
                    .clientId(naverClientId)
                    .clientSecret(naverClientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(REDIRECT_URI)
                    .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                    .tokenUri("https://nid.naver.com/oauth2.0/token")
                    .userInfoUri("https://openapi.naver.com/v1/nid/me")
                    .userNameAttributeName("response")
                    .build());
        }

        return list;
    }

    // 프론트에서 소셜 로그인 버튼 노출 여부를 정할 때 사용
    public List<String> enabledProviders() {
        return registrations().stream().map(ClientRegistration::getRegistrationId).toList();
    }
}
