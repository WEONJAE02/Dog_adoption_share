package org.likelion.hsu.db_project.Config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Service.SocialUserService;
import org.likelion.hsu.db_project.Service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SocialLoginProperties socialLoginProperties;
    private final SocialUserService socialUserService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // GlobalCorsConfig의 corsFilter 빈을 그대로 사용
                .cors(Customizer.withDefaults())
                // 프론트가 다른 포트에서 호출하는 구조라 CSRF 토큰 대신
                // SameSite=Lax 세션 쿠키 + CORS 허용 출처 제한으로 방어한다
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 조회는 누구나, 등록/수정/삭제는 로그인 필요
                        .requestMatchers(HttpMethod.GET, "/dogs/**", "/uploads/**").permitAll()
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .anyRequest().authenticated())
                // 로그인 페이지로 보내지 않고 401/403 JSON 응답
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeMessage(response, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeMessage(response, HttpStatus.FORBIDDEN, "작성자만 수정·삭제할 수 있습니다.")))
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())));

        // 키가 설정된 제공자가 있을 때만 소셜 로그인 활성화
        List<ClientRegistration> registrations = socialLoginProperties.registrations();
        if (!registrations.isEmpty()) {
            http.oauth2Login(oauth -> oauth
                    .clientRegistrationRepository(new InMemoryClientRegistrationRepository(registrations))
                    .userInfoEndpoint(userInfo -> userInfo.userService(socialUserService))
                    .successHandler((request, response, authentication) ->
                            response.sendRedirect(frontendUrl))
                    .failureHandler((request, response, ex) ->
                            response.sendRedirect(frontendUrl + "?loginError=social")));
        }

        return http.build();
    }

    private void writeMessage(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 이메일 로그인(/auth/login)에서 사용
    @Bean
    public AuthenticationManager authenticationManager(UserService userService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
