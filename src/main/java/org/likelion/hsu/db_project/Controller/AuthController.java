package org.likelion.hsu.db_project.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Config.SocialLoginProperties;
import org.likelion.hsu.db_project.Dto.LoginRequestDto;
import org.likelion.hsu.db_project.Dto.SignupRequestDto;
import org.likelion.hsu.db_project.Dto.UserResponseDto;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SocialLoginProperties socialLoginProperties;

    /**
     * ✅ 이메일 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody SignupRequestDto dto) {
        try {
            userService.signup(dto);
        } catch (IllegalArgumentException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return message(HttpStatus.CONFLICT, e.getMessage());
        }
        return message(HttpStatus.CREATED, "회원가입이 완료되었습니다.");
    }

    /**
     * ✅ 이메일 로그인 (세션 발급)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequestDto dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (dto.getEmail() == null || dto.getPassword() == null) {
            return message(HttpStatus.BAD_REQUEST, "이메일과 비밀번호를 입력해주세요.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(dto.getEmail(), dto.getPassword()));
        } catch (AuthenticationException e) {
            return message(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 세션 고정 공격 방지: 로그인 시 세션 ID 교체
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(UserResponseDto.from((LoginUser) authentication.getPrincipal()));
    }

    /**
     * ✅ 현재 로그인 상태 + 사용 가능한 소셜 로그인 목록
     * (로그아웃은 SecurityConfig의 POST /auth/logout 에서 처리)
     */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal LoginUser user) {
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", user != null);
        body.put("user", user != null ? UserResponseDto.from(user) : null);
        body.put("socialProviders", socialLoginProperties.enabledProviders());
        return body;
    }

    private ResponseEntity<Map<String, Object>> message(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
