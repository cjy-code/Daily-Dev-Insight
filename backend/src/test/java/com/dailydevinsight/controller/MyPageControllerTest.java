package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import com.dailydevinsight.dto.MyPageActivityDTO;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.service.MyPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MyPageController.class)
@Import(SecurityConfig.class)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyPageService myPageService;

    @MockBean
    private UserDetailsService userDetailsService;

    /**
     * @date 2026-04-20
     * @desc 인증 사용자 요청 시 마이페이지 메인 화면이 렌더링되는지 검증합니다.
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void myPageMain_WithAuthenticatedUser_ShouldRenderMainView() throws Exception {
        given(myPageService.getMyProfile(anyString())).willReturn(createUser());
        given(myPageService.getMyActivity(anyString())).willReturn(createActivity());

        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk())
                .andExpect(view().name("mypage/main"));
    }

    /**
     * @date 2026-04-20
     * @desc 비인증 사용자 접근 시 로그인 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void myPageMain_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/mypage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    /**
     * @date 2026-04-20
     * @desc 비밀번호 변경 요청이 성공하면 비밀번호 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void changePassword_WithValidRequest_ShouldRedirectPasswordPage() throws Exception {
        doNothing().when(myPageService).changePassword(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/mypage/password")
                        .with(csrf())
                        .param("currentPassword", "oldpass123")
                        .param("newPassword", "newpass123")
                        .param("newPasswordConfirm", "newpass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage/password"));
    }

    /**
     * @date 2026-04-20
     * @desc 회원탈퇴 요청이 성공하면 로그인 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void withdraw_WithValidRequest_ShouldRedirectLoginPage() throws Exception {
        doNothing().when(myPageService).withdraw(anyString(), anyString());

        mockMvc.perform(post("/mypage/withdraw")
                        .with(csrf())
                        .param("currentPassword", "oldpass123")
                        .param("agreeWithdraw", "Y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?withdraw"));
    }

    /**
     * @date 2026-04-20
     * @desc 테스트용 사용자 엔티티를 생성합니다.
     */
    private User createUser() {
        return User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@example.com")
                .password("password")
                .name("테스트사용자")
                .role("USER")
                .status("ACTIVE")
                .build();
    }

    /**
     * @date 2026-04-20
     * @desc 테스트용 활동 DTO를 생성합니다.
     */
    private MyPageActivityDTO createActivity() {
        return MyPageActivityDTO.builder()
                .bookmarkItems(Collections.emptyList())
                .likeItems(Collections.emptyList())
                .build();
    }
}
