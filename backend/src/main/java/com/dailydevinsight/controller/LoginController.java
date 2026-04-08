package com.dailydevinsight.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
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
        return "views/login";
    }
}
