
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.repositories.Teachers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import com.laf.cobabe_code_lms.services.TeacherEmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/teacher/account")
public class TeacherAccountController {

    private final Teachers teachers;
    private final PasswordEncoder passwordEncoder;
    private final TeacherEmailVerificationService verificationService;


    public TeacherAccountController(
            Teachers teachers,
            PasswordEncoder passwordEncoder, TeacherEmailVerificationService verificationService) {

        this.teachers = teachers;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
    }

    @GetMapping
    public String accountPage(
            Authentication authentication,
            Model model) {

        Teacher teacher = getTeacher(authentication);

        model.addAttribute(
                "recoveryEmail", teacher.recoveryEmail);
        model.addAttribute(
                "emailVerified", teacher.recoveryEmailVerified);

        return "teacher-account";
    }

    @PostMapping("/send-verification")
    public String sendVerification(
            Authentication authentication,
            Model model) {

        Teacher teacher = getTeacher(authentication);

        if (teacher.recoveryEmail == null ||
                teacher.recoveryEmail.isBlank()) {

            return showError(
                    model,
                    teacher,
                    "Please configure a recovery email first.");
        }

        if (teacher.recoveryEmailVerified) {
            return "redirect:/teacher/account?alreadyVerified";
        }

        try {
            verificationService.sendVerificationEmail(teacher);
        } catch (org.springframework.mail.MailException e) {
            return showError(
                    model,
                    teacher,
                    "Unable to send verification email. Please try again.");
        }

        return "redirect:/teacher/account?verificationSent";
    }

    @PostMapping("/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        Teacher teacher = getTeacher(authentication);

        if (!passwordEncoder.matches(
                currentPassword, teacher.passwordHash)) {

            return showError(model, teacher,
                    "Current password is incorrect.");
        }

        if (newPassword.length() < 8) {
            return showError(model, teacher,
                    "New password must contain at least 8 characters.");
        }

        if (!newPassword.equals(confirmPassword)) {
            return showError(model, teacher,
                    "New passwords do not match.");
        }

        if (passwordEncoder.matches(
                newPassword, teacher.passwordHash)) {

            return showError(model, teacher,
                    "New password must be different from your current password.");
        }

        // Save only the BCrypt hash, never the plaintext password.
        teacher.passwordHash = passwordEncoder.encode(newPassword);
        teachers.save(teacher);

        // End the current session after changing credentials.
        new SecurityContextLogoutHandler().logout(
                request, response, authentication);

        return "redirect:/?passwordChanged";
    }

    private String showError(
            Model model,
            Teacher teacher,
            String message) {

        model.addAttribute("error", message);
        model.addAttribute(
                "recoveryEmail", teacher.recoveryEmail);
        model.addAttribute(
                "emailVerified", teacher.recoveryEmailVerified);

        return "teacher-account";
    }

    private Teacher getTeacher(Authentication authentication) {

        if (authentication == null ||
                authentication.getAuthorities().stream()
                        .noneMatch(a ->
                                a.getAuthority().equals("ROLE_TEACHER"))) {

            throw new org.springframework.web.server
                    .ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }

        return teachers.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new org.springframework.web.server
                                .ResponseStatusException(
                                org.springframework.http.HttpStatus.FORBIDDEN));
    }
}
