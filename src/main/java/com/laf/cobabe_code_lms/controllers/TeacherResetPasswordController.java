
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.TeacherPasswordResetService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TeacherResetPasswordController {

    private final TeacherPasswordResetService resetService;

    public TeacherResetPasswordController(
            TeacherPasswordResetService resetService) {
        this.resetService = resetService;
    }

    @GetMapping("/teacher/reset-password")
    public String showResetPage(
            @RequestParam(required = false) String token,
            Model model) {

        if (!resetService.isValidToken(token)) {
            model.addAttribute("error",
                    "This password reset link is invalid, " +
                            "expired, or has already been used.");

            return "teacher-reset-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("validToken", true);

        return "teacher-reset-password";
    }

    @PostMapping("/teacher/reset-password")
    public String resetPassword(
            @RequestParam(required = false) String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        if (!resetService.isValidToken(token)) {
            model.addAttribute("error",
                    "This password reset link is invalid, " +
                            "expired, or has already been used.");

            return "teacher-reset-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("validToken", true);

        // Match existing teacher password requirements.
        if (password.length() < 6) {
            model.addAttribute("error",
                    "Password must contain at least 6 characters.");

            return "teacher-reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error",
                    "Passwords do not match.");

            return "teacher-reset-password";
        }

        boolean success = resetService.resetPassword(
                token, password);

        if (!success) {
            model.addAttribute("validToken", false);
            model.addAttribute("error",
                    "Unable to reset password. " +
                            "Please request a new reset link.");

            return "teacher-reset-password";
        }

        model.addAttribute("validToken", false);
        model.addAttribute("success",
                "Your password has been reset successfully! " +
                        "You can now log in with your new password.");

        return "teacher-reset-password";
    }
}
