
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.Students;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/password")
public class StudentPasswordController {

    private final Students students;
    private final PasswordEncoder encoder;

    public StudentPasswordController(
            Students students,
            PasswordEncoder encoder) {

        this.students = students;
        this.encoder = encoder;
    }

    @GetMapping
    public String passwordPage(
            Authentication authentication,
            Model model) {

        Student student = students
                .findByUsername(authentication.getName())
                .orElseThrow();

        model.addAttribute(
                "firstLogin", student.mustChangePassword);

        return "student-password";
    }

    @PostMapping
    public String changePassword(
            Authentication authentication,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirect) {

        Student student = students
                .findByUsername(authentication.getName())
                .orElseThrow();

        if (!encoder.matches(
                currentPassword, student.passwordHash)) {

            redirect.addFlashAttribute(
                    "error", "Current password is incorrect.");

            return "redirect:/student/password";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute(
                    "error", "New passwords do not match.");

            return "redirect:/student/password";
        }

        if (newPassword.length() < 8 ||
                newPassword.length() > 128) {

            redirect.addFlashAttribute(
                    "error",
                    "Password must be between 8 and 128 characters.");

            return "redirect:/student/password";
        }

        if (encoder.matches(newPassword, student.passwordHash)) {
            redirect.addFlashAttribute(
                    "error",
                    "Choose a password different from your current one.");

            return "redirect:/student/password";
        }

        student.passwordHash = encoder.encode(newPassword);
        student.mustChangePassword = false;

        students.save(student);

        redirect.addFlashAttribute(
                "success", "Password updated successfully!");

        return "redirect:/student";
    }
}
