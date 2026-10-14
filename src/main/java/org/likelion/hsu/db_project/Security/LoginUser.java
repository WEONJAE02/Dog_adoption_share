package org.likelion.hsu.db_project.Security;

import lombok.Getter;
import org.likelion.hsu.db_project.Entity.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 세션에 저장되는 로그인 사용자.
 * 이메일 로그인(UserDetails)과 소셜 로그인(OAuth2User)을 같은 타입으로 다룬다.
 */
@Getter
public class LoginUser implements UserDetails, OAuth2User, CredentialsContainer, Serializable {

    private final Long userId;
    private final String provider;
    private final String providerId;
    private final String email;
    private final String nickname;
    private String password;

    public LoginUser(User user) {
        this.userId = user.getUserId();
        this.provider = user.getProvider();
        this.providerId = user.getProviderId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.password = user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return provider + ":" + providerId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("userId", userId);
    }

    // 인증이 끝나면 비밀번호 해시가 세션에 남지 않도록 지운다
    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
