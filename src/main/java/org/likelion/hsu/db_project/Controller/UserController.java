package org.likelion.hsu.db_project.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.likelion.hsu.db_project.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 내 정보 (모두 로그인 필요)
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private static final String WITHDRAW_CONFIRM = "탈퇴합니다";

    private final UserService userService;

    /**
     * ✅ 내 정보 + 활동 수
     */
    @GetMapping
    public Map<String, Object> profile(@AuthenticationPrincipal LoginUser user) {
        return userService.profile(user.getUserId());
    }

    /**
     * ✅ 닉네임 변경
     */
    @PutMapping("/nickname")
    public ResponseEntity<Map<String, Object>> updateNickname(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        try {
            userService.updateNickname(user.getUserId(), body.get("nickname"));
        } catch (IllegalArgumentException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return message(HttpStatus.OK, "닉네임을 변경했습니다.");
    }

    /**
     * ✅ 회원 탈퇴 (확인 문구 "탈퇴합니다" 필요) → 로그아웃
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> withdraw(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LoginUser user,
            HttpServletRequest request
    ) {
        if (!WITHDRAW_CONFIRM.equals(body.get("confirm"))) {
            return message(HttpStatus.BAD_REQUEST, "확인 문구를 정확히 입력해주세요.");
        }
        userService.withdraw(user.getUserId());

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return message(HttpStatus.OK, "탈퇴가 완료되었습니다.");
    }

    private ResponseEntity<Map<String, Object>> message(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
