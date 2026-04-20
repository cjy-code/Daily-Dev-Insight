package com.dailydevinsight.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    /**
     * @date 2026-04-15
     * @desc 사용자 로그인 페이지를 렌더링합니다.
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "withdraw", required = false) String withdraw,
            RedirectAttributes redirectAttributes
    ) {
        if (error != null) {
            redirectAttributes.addFlashAttribute("loginError", true);
            return "redirect:/login";
        }
        if (logout != null) {
            redirectAttributes.addFlashAttribute("logoutSuccess", true);
            return "redirect:/login";
        }
        if (withdraw != null) {
            redirectAttributes.addFlashAttribute("withdrawSuccess", true);
            return "redirect:/login";
        }
        return "views/login";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 로그인 페이지를 렌더링합니다.
     */
    @GetMapping("/admin/login")
    public String adminLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            RedirectAttributes redirectAttributes
    ) {
        if (error != null) {
            redirectAttributes.addFlashAttribute("adminLoginError", true);
            return "redirect:/admin/login";
        }
        if (logout != null) {
            redirectAttributes.addFlashAttribute("adminLogoutSuccess", true);
            return "redirect:/admin/login";
        }
        return "views/admin-login";
    }
}
