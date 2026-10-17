
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.TeacherPasswordResetService;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TeacherForgotPasswordController {

    private final TeacherPasswordResetService resetService;

    public TeacherForgotPasswordController(
            TeacherPasswordResetService resetService) {
        this.resetService = resetService;
    }

    @GetMapping("/teacher/forgot-password")
    public String showForgotPasswordPage() {
        return "teacher-forgot-password";
    }

    @PostMapping("/teacher/forgot-password")
    public String requestReset(
            @RequestParam String email,
            Model model) {

        try {
            resetService.requestPasswordReset(email);
        } catch (MailException e) {
            // Don't reveal whether the email belongs to an account.
            // Log the failure securely in a future hardening step.
        }

        model.addAttribute("success",
                "If a verified teacher account uses that email, " +
                        "a password reset link will be sent.");

        return "teacher-forgot-password";
    }
}
