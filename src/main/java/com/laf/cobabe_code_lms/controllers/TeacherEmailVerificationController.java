
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.services.TeacherEmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TeacherEmailVerificationController {

    private final TeacherEmailVerificationService verificationService;

    public TeacherEmailVerificationController(
            TeacherEmailVerificationService verificationService) {

        this.verificationService = verificationService;
    }

    @GetMapping("/teacher/verify-email")
    public String verifyEmail(
            @RequestParam(required = false) String token,
            Model model) {

        boolean verified = verificationService.verifyToken(token);

        if (verified) {
            model.addAttribute("success",
                    "Your recovery email has been verified successfully!");
        } else {
            model.addAttribute("error",
                    "This verification link is invalid, expired, or has already been used.");
        }

        return "teacher-email-verification";
    }
}
