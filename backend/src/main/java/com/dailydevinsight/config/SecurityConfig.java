package com.dailydevinsight.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.util.Objects;

@Configuration
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String USER_LOGIN_PATH = "/login";
    private static final String ADMIN_LOGIN_PATH = "/admin/login";
    private static final String USER_SUCCESS_PATH = "/";
    private static final String ADMIN_SUCCESS_PATH = "/admin";
    private static final String ADMIN_DENIED_PATH = "/admin/login?adminDenied=true";
    private static final String ADMIN_CSRF_ERROR_PATH = "/admin/login?csrfError=true";
    private static final String LOGOUT_PATH = "/auth/logout";

    /**
     * @date 2026-04-15
     * @desc 관리자 경로 보안을 위한 필터 체인을 구성합니다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService
    ) throws Exception {
        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ADMIN_LOGIN_PATH, "/error", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage(ADMIN_LOGIN_PATH)
                        .loginProcessingUrl(ADMIN_LOGIN_PATH)
                        .successHandler((request, response, authentication) ->
                                processRoleBoundLoginSuccess(request, response, authentication, ROLE_ADMIN, ADMIN_SUCCESS_PATH, ADMIN_LOGIN_PATH))
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                processAdminAccessDenied(request, response, accessDeniedException))
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * @date 2026-04-15
     * @desc 사용자 경로 보안을 위한 필터 체인을 구성합니다.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(USER_LOGIN_PATH, ADMIN_LOGIN_PATH, "/error", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .requestMatchers(LOGOUT_PATH).authenticated()
                        .anyRequest().hasRole("USER")
                )
                .formLogin(form -> form
                        .loginPage(USER_LOGIN_PATH)
                        .loginProcessingUrl(USER_LOGIN_PATH)
                        .successHandler((request, response, authentication) ->
                                processRoleBoundLoginSuccess(request, response, authentication, ROLE_USER, USER_SUCCESS_PATH, USER_LOGIN_PATH))
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * @date 2026-04-15
     * @desc 현재 프로젝트 정책에 맞는 비밀번호 인코더를 제공합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * @date 2026-04-21
     * @desc 로그인 엔드포인트에 맞는 역할인지 확인하고 불일치 시 로그아웃 후 에러 페이지로 이동시킵니다.
     */
    private void processRoleBoundLoginSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            String requiredRole,
            String successRedirectPath,
            String loginPath
    ) throws IOException {
        if (hasAuthority(authentication, requiredRole)) {
            response.sendRedirect(successRedirectPath);
            return;
        }

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        response.sendRedirect(loginPath + "?error");
    }

    /**
     * @date 2026-04-21
     * @desc 인증 사용자 권한 목록에 지정된 권한 문자열이 포함되어 있는지 확인합니다.
     */
    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(authority::equals);
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 경로 접근 거부 시 CSRF 실패와 권한 부족을 구분하여 적절한 화면으로 이동시킵니다.
     */
    private void processAdminAccessDenied(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        if (accessDeniedException instanceof CsrfException) {
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("/admin/posts/")) {
                response.sendRedirect(referer + (referer.contains("?") ? "&" : "?") + "csrfError=true");
                return;
            }
            response.sendRedirect(ADMIN_CSRF_ERROR_PATH);
            return;
        }
        response.sendRedirect(ADMIN_DENIED_PATH);
    }
}
