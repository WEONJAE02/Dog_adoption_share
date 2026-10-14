package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.SignupRequestDto;
import org.likelion.hsu.db_project.Entity.User;
import org.likelion.hsu.db_project.Repository.UserRepository;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    public static final String LOCAL = "local";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 이메일 회원가입
    public void signup(SignupRequestDto dto) {
        String email = normalizeEmail(dto.getEmail());
        String password = dto.getPassword();
        String nickname = dto.getNickname() != null ? dto.getNickname().trim() : "";

        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        // BCrypt는 72바이트까지만 처리한다
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (nickname.isEmpty() || nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 1~20자로 입력해주세요.");
        }
        if (userRepository.findByProviderAndProviderId(LOCAL, email).isPresent()) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .provider(LOCAL)
                .providerId(email)
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
    }

    // ✅ 이메일 로그인 시 사용자 조회 (Spring Security)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByProviderAndProviderId(LOCAL, normalizeEmail(email))
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("가입되지 않은 이메일입니다."));
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }
}
