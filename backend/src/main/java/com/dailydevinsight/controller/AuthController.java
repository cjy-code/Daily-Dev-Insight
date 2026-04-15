package com.dailydevinsight.controller;

import com.dailydevinsight.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        authService.logout(request, response, authentication);
        return "redirect:" + resolveLogoutRedirectUrl(authentication);
    }

    /**
     * @date 2026-04-15
     * @desc 로그인 사용자 권한에 따라 로그아웃 후 이동 경로를 반환합니다.
     */
    private String resolveLogoutRedirectUrl(Authentication authentication) {
        if (authentication == null) {
            return "/login?logout";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        return isAdmin ? "/admin/login?logout" : "/login?logout";
    }
}
