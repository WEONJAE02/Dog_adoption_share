package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.ConsentRequestDto;
import org.likelion.hsu.db_project.Dto.SignupRequestDto;
import org.likelion.hsu.db_project.Repository.DogRepository;
import org.likelion.hsu.db_project.Repository.InquiryRepository;
import org.likelion.hsu.db_project.Repository.ReportRepository;
import org.likelion.hsu.db_project.Entity.User;
import org.likelion.hsu.db_project.Repository.UserRepository;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    public static final String LOCAL = "local";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DogRepository dogRepository;
    private final DogService dogService;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;

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
        validateNickname(nickname);
        if (!dto.allAgreed()) {
            throw new IllegalArgumentException("필수 항목(만 14세 이상, 이용약관, 개인정보 수집·이용)에 모두 동의해주세요.");
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
                .termsAgreedAt(LocalDateTime.now())
                .build();

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
    }

    // ✅ 약관 동의 (소셜 로그인으로 처음 가입한 회원, 동의 기능 도입 전 회원)
    public void agree(Long userId, ConsentRequestDto dto) {
        if (!dto.allAgreed()) {
            throw new IllegalArgumentException("필수 항목(만 14세 이상, 이용약관, 개인정보 수집·이용)에 모두 동의해주세요.");
        }
        userRepository.agreeTerms(userId);
    }

    // ✅ 내 정보 + 활동 수
    public Map<String, Object> profile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getUserId());
        body.put("provider", user.getProvider());
        body.put("email", user.getEmail());
        body.put("nickname", user.getNickname());
        body.put("createdAt", user.getCreatedAt());
        body.put("termsAgreedAt", user.getTermsAgreedAt());
        body.put("counts", userRepository.countActivities(userId));
        return body;
    }

    public void updateNickname(Long userId, String nickname) {
        String value = nickname != null ? nickname.trim() : "";
        validateNickname(value);
        userRepository.updateNickname(userId, value);
    }

    // ✅ 회원 탈퇴
    // 등록한 유기견(사진 포함)·주고받은 입양 문의·신고 기록은 삭제하고,
    // 커뮤니티 글·댓글은 남기되 작성자를 "탈퇴한 회원"으로 바꾼다
    @Transactional
    public void withdraw(Long userId) {
        for (Long dogId : dogRepository.findIdsByUserId(userId)) {
            dogService.deleteDog(dogId, userId);
        }
        inquiryRepository.deleteByUserId(userId);
        reportRepository.deleteByReporterId(userId);
        userRepository.anonymize(userId);
    }

    private void validateNickname(String nickname) {
        if (nickname.isEmpty() || nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 1~20자로 입력해주세요.");
        }
        if ("탈퇴한 회원".equals(nickname)) {
            throw new IllegalArgumentException("사용할 수 없는 닉네임입니다.");
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
