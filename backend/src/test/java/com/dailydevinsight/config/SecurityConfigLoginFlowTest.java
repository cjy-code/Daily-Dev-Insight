package com.dailydevinsight.config;

import com.dailydevinsight.controller.LoginController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
class SecurityConfigLoginFlowTest {

    private static final String ENCODED_PW =
            PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("pw");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    /**
     * @date 2026-04-21
     * @desc 테스트용 사용자/관리자 계정 조회 동작을 공통으로 설정합니다.
     */
    @BeforeEach
    void setUpUserDetailsService() {
        UserDetails adminUser = User.builder()
                .username("admin")
                .password(ENCODED_PW)
                .roles("ADMIN")
                .build();

        UserDetails normalUser = User.builder()
                .username("user")
                .password(ENCODED_PW)
                .roles("USER")
                .build();

        given(userDetailsService.loadUserByUsername(anyString())).willAnswer(invocation -> {
            String username = invocation.getArgument(0);
            if ("admin".equals(username)) {
                return adminUser;
            }
            if ("user".equals(username)) {
                return normalUser;
            }
            throw new UsernameNotFoundException("not found: " + username);
        });
    }

    /**
     * @date 2026-04-21
     * @desc 사용자 로그인 페이지에서는 USER 권한 계정 로그인만 허용되어 메인 페이지로 이동합니다.
     */
    @Test
    void userLogin_WithUserRole_ShouldRedirectToIndex() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("user"));
    }

    /**
     * @date 2026-04-21
     * @desc 사용자 로그인 페이지에서 ADMIN 권한 계정 로그인 시 에러 페이지로 되돌리고 인증을 유지하지 않습니다.
     */
    @Test
    void userLogin_WithAdminRole_ShouldRedirectToUserLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    /**
     * @date 2026-04-21
     * @desc 관리자 로그인 페이지에서는 ADMIN 권한 계정 로그인만 허용되어 관리자 메인으로 이동합니다.
     */
    @Test
    void adminLogin_WithAdminRole_ShouldRedirectToAdminRoot() throws Exception {
        mockMvc.perform(formLogin("/admin/login").user("admin").password("pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername("admin"));
    }

    /**
     * @date 2026-04-21
     * @desc 관리자 로그인 페이지에서 USER 권한 계정 로그인 시 에러 페이지로 되돌리고 인증을 유지하지 않습니다.
     */
    @Test
    void adminLogin_WithUserRole_ShouldRedirectToAdminLoginError() throws Exception {
        mockMvc.perform(formLogin("/admin/login").user("user").password("pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"))
                .andExpect(unauthenticated());
    }
}
