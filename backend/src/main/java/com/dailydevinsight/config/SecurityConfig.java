package com.dailydevinsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * @date 2026-04-15
     * @desc 보안 필터 체인을 구성하고 관리자 경로 접근 권한을 설정합니다.
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
                        .requestMatchers("/admin/login", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true)
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/?adminDenied=true"))
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * @date 2026-04-15
     * @desc 일반 사용자 영역 보안 필터 체인을 구성합니다.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/admin/login", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * @date 2026-04-15
     * @desc 현재 프로젝트 로그인 정책에 맞는 비밀번호 인코더를 제공합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
