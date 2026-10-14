package org.likelion.hsu.db_project.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Repository.UserRepository;
import org.likelion.hsu.db_project.Security.LoginUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 약관에 동의하지 않은 회원(소셜 로그인 첫 가입 등)은 글쓰기·문의·신고 같은 쓰기 요청을 할 수 없다.
 * 조회, 로그인 관련(/auth/**), 내 정보·탈퇴(/users/me)는 허용한다.
 */
@Configuration
@RequiredArgsConstructor
public class ConsentConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String method = request.getMethod();
                String path = request.getRequestURI();
                if ("GET".equals(method) || "OPTIONS".equals(method)
                        || path.startsWith("/auth/") || path.startsWith("/users/me")) {
                    return true;
                }

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof LoginUser user
                        && !userRepository.isTermsAgreed(user.getUserId())) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"이용약관과 개인정보 수집·이용에 동의해야 이용할 수 있습니다.\"}");
                    return false;
                }
                return true;
            }
        });
    }
}
