package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void login_ShouldRenderLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/login"));
    }

    @Test
    void login_WithErrorParam_ShouldRedirectToCleanLoginUrl() throws Exception {
        mockMvc.perform(get("/login").param("error", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("loginError", true));
    }

    @Test
    void login_WithLogoutParam_ShouldRedirectToCleanLoginUrl() throws Exception {
        mockMvc.perform(get("/login").param("logout", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("logoutSuccess", true));
    }

    @Test
    void login_WithWithdrawParam_ShouldRedirectToCleanLoginUrl() throws Exception {
        mockMvc.perform(get("/login").param("withdraw", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("withdrawSuccess", true));
    }

    @Test
    void adminLogin_ShouldRenderAdminLoginView() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/admin-login"));
    }

    @Test
    void adminLogin_WithErrorParam_ShouldRedirectToCleanAdminLoginUrl() throws Exception {
        mockMvc.perform(get("/admin/login").param("error", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(flash().attribute("adminLoginError", true));
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 권한 부족 파라미터가 전달되면 안내 메시지 플래시와 함께 관리자 로그인 페이지로 리다이렉트하는지 검증합니다.
     */
    @Test
    void adminLogin_WithAdminDeniedParam_ShouldRedirectToCleanAdminLoginUrl() throws Exception {
        mockMvc.perform(get("/admin/login").param("adminDenied", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(flash().attribute("adminDeniedError", true));
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 CSRF 실패 파라미터가 전달되면 안내 메시지 플래시와 함께 관리자 로그인 페이지로 리다이렉트하는지 검증합니다.
     */
    @Test
    void adminLogin_WithCsrfErrorParam_ShouldRedirectToCleanAdminLoginUrl() throws Exception {
        mockMvc.perform(get("/admin/login").param("csrfError", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(flash().attribute("adminCsrfError", true));
    }
}
