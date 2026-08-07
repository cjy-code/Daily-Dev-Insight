package com.dailydevinsight.controller;

import com.dailydevinsight.dto.MyPageActivityDTO;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.service.MyPageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * @date 2026-08-06
     * @desc 마이페이지 메인 화면을 렌더링합니다.
     */
    @GetMapping
    public String myPageMain(Authentication authentication, Model model) {
        String loginUserId = resolveLoginUserId(authentication);
        User profile = myPageService.getMyProfile(loginUserId);
        MyPageActivityDTO activity = myPageService.getMyActivity(loginUserId, 0, 0);

        model.addAttribute("profile", profile);
        model.addAttribute("activity", activity);
        model.addAttribute("currentMenu", "main");
        return "mypage/main";
    }

    /**
     * @date 2026-04-20
     * @desc 회원정보 수정 화면을 렌더링합니다.
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String loginUserId = resolveLoginUserId(authentication);
        User profile = myPageService.getMyProfile(loginUserId);

        model.addAttribute("profile", profile);
        model.addAttribute("currentMenu", "profile");
        return "mypage/profile";
    }

    /**
     * @date 2026-04-20
     * @desc 비밀번호 변경 화면을 렌더링합니다.
     */
    @GetMapping("/password")
    public String password(Authentication authentication, Model model) {
        User profile = myPageService.getMyProfile(resolveLoginUserId(authentication));

        model.addAttribute("profile", profile);
        model.addAttribute("currentMenu", "password");
        return "mypage/password";
    }

    /**
     * @date 2026-08-06
     * @desc 북마크/좋아요 활동 화면을 페이지 단위로 렌더링합니다.
     */
    @GetMapping("/activity")
    public String activity(
            Authentication authentication,
            @RequestParam(value = "bookmarkPage", defaultValue = "0") int bookmarkPage,
            @RequestParam(value = "likePage", defaultValue = "0") int likePage,
            Model model
    ) {
        String loginUserId = resolveLoginUserId(authentication);
        User profile = myPageService.getMyProfile(loginUserId);
        MyPageActivityDTO activity = myPageService.getMyActivity(loginUserId, bookmarkPage, likePage);

        model.addAttribute("profile", profile);
        model.addAttribute("activity", activity);
        model.addAttribute("currentMenu", "activity");
        return "mypage/activity";
    }

    /**
     * @date 2026-04-20
     * @desc 회원탈퇴 화면을 렌더링합니다.
     */
    @GetMapping("/withdraw")
    public String withdraw(Authentication authentication, Model model) {
        User profile = myPageService.getMyProfile(resolveLoginUserId(authentication));

        model.addAttribute("profile", profile);
        model.addAttribute("currentMenu", "withdraw");
        return "mypage/withdraw";
    }

    /**
     * @date 2026-04-20
     * @desc 회원정보(이름/이메일) 수정 요청을 처리합니다.
     */
    @PostMapping("/profile")
    public String updateProfile(
            Authentication authentication,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes
    ) {
        try {
            myPageService.updateProfile(resolveLoginUserId(authentication), name, email);
            redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/mypage/profile";
    }

    /**
     * @date 2026-04-20
     * @desc 비밀번호 변경 요청을 처리합니다.
     */
    @PostMapping("/password")
    public String changePassword(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("newPasswordConfirm") String newPasswordConfirm,
            RedirectAttributes redirectAttributes
    ) {
        try {
            myPageService.changePassword(
                    resolveLoginUserId(authentication),
                    currentPassword,
                    newPassword,
                    newPasswordConfirm
            );
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/mypage/password";
    }

    /**
     * @date 2026-04-20
     * @desc 회원탈퇴 요청을 처리하고 로그아웃을 수행합니다.
     */
    @PostMapping("/withdraw")
    public String processWithdraw(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam(value = "agreeWithdraw", required = false) String agreeWithdraw,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        if (!"Y".equalsIgnoreCase(agreeWithdraw)) {
            redirectAttributes.addFlashAttribute("errorMessage", "탈퇴 동의 체크가 필요합니다.");
            return "redirect:/mypage/withdraw";
        }

        try {
            myPageService.withdraw(resolveLoginUserId(authentication), currentPassword);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?withdraw";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/mypage/withdraw";
        }
    }

    /**
     * @date 2026-08-07
     * @desc 마이페이지 GET 흐름의 미인증 접근 예외를 로그인 화면 리다이렉트로 처리합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleUnauthenticatedAccess(
            IllegalArgumentException exception,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/login";
    }

    /**
     * @date 2026-04-20
     * @desc 인증 객체에서 로그인 user_id를 추출합니다.
     */
    private String resolveLoginUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("로그인 사용자 정보가 없습니다.");
        }
        return authentication.getName();
    }
}
