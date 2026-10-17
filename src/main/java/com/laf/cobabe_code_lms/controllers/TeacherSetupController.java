
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.repositories.Teachers;
import com.laf.cobabe_code_lms.repositories.Students;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Locale;

@Controller
@RequestMapping("/teacher/setup")
public class TeacherSetupController {

    private final Teachers teachers;
    private final Students students;
    private final PasswordEncoder passwordEncoder;

    public TeacherSetupController(
            Teachers teachers,
            Students students,
            PasswordEncoder passwordEncoder) {

        this.teachers = teachers;
        this.students = students;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String setupPage(
            Authentication authentication,
            Model model) {

        Teacher teacher = getTeacher(authentication);

        if (!teacher.mustCompleteSetup) {
            return "redirect:/teacher";
        }

        return "teacher-setup";
    }

    @PostMapping
    public String completeSetup(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String recoveryEmail,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        Teacher teacher = getTeacher(authentication);

        if (!teacher.mustCompleteSetup) {
            return "redirect:/teacher";
        }

        username = username.trim();
        recoveryEmail = recoveryEmail.trim()
                .toLowerCase(Locale.ROOT);

        if (username.length() < 3 || username.length() > 50) {
            model.addAttribute("error",
                    "Username must contain 3–50 characters.");
            return "teacher-setup";
        }

        if (!username.matches("[A-Za-z0-9._-]+")) {
            model.addAttribute("error",
                    "Username may contain only letters, numbers, dots, underscores, and hyphens.");
            return "teacher-setup";
        }

        if (password.length() < 6) {
            model.addAttribute("error",
                    "Password must contain at least 6 characters.");
            return "teacher-setup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error",
                    "Passwords do not match.");
            return "teacher-setup";
        }

        if (recoveryEmail.length() > 254 ||
                !recoveryEmail.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            model.addAttribute("error",
                    "Enter a valid recovery email address.");
            return "teacher-setup";
        }

        // Prevent conflicts with student usernames.
        if (students.findByUsername(username).isPresent()) {
            model.addAttribute("error",
                    "That username is already in use.");
            return "teacher-setup";
        }

        // Prevent conflicts with other teacher accounts.
        boolean teacherUsernameTaken =
                teachers.findByUsername(username)
                        .filter(existing ->
                                !existing.id.equals(teacher.id))
                        .isPresent();

        if (teacherUsernameTaken) {
            model.addAttribute("error",
                    "That username is already in use.");
            return "teacher-setup";
        }

        teacher.username = username;
        teacher.passwordHash = passwordEncoder.encode(password);
        teacher.recoveryEmail = recoveryEmail;
        teacher.recoveryEmailVerified = false;
        teacher.mustCompleteSetup = false;

        teachers.save(teacher);

        // End the initial session so the teacher must
        // sign in again with the new credentials.
        new SecurityContextLogoutHandler().logout(
                request, response, authentication);

        return "redirect:/?setupComplete";
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
