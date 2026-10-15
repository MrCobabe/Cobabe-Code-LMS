
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.ClassPeriods;
import com.laf.cobabe_code_lms.repositories.Submissions;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Controller
@RequestMapping("/teacher/students")
public class StudentManagementController {

    private final Students students;
    private final ClassPeriods classes;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    private final Submissions submissions;
    private final AssignmentDrafts drafts;
    private final SandboxPrograms sandboxes;

    public StudentManagementController(
            Students students,
            ClassPeriods classes,
            PasswordEncoder encoder,
            Submissions submissions,
            AssignmentDrafts drafts,
            SandboxPrograms sandboxes) {

        this.students = students;
        this.classes = classes;
        this.encoder = encoder;
        this.submissions = submissions;
        this.drafts = drafts;
        this.sandboxes = sandboxes;
    }

    @GetMapping
    public String page(
            @RequestParam(required = false) Long classId,
            Model model) {

        model.addAttribute("classes", classes.findAll());
        model.addAttribute("selectedClassId", classId);

        var list = classId == null
                ? students.findAll()
                : students.findByClassPeriodId(classId);

        model.addAttribute("students", list);
        return "students";
    }

    @PostMapping("/create")
    public String create(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam Long classId,
            RedirectAttributes redirect) {

        name = name.trim();
        username = username.trim().toLowerCase(Locale.ROOT);

        if (name.isBlank() || name.length() > 120 ||
                !username.matches("[a-z0-9._-]{3,40}")) {
            redirect.addFlashAttribute(
                    "error", "Invalid name or username.");
            return "redirect:/teacher/students";
        }

        if (students.findByUsername(username).isPresent()
                || username.equals("teacher")) {
            redirect.addFlashAttribute(
                    "error", "Username already exists.");
            return "redirect:/teacher/students";
        }

        var classPeriod = classes.findById(classId).orElse(null);

        if (classPeriod == null) {
            redirect.addFlashAttribute(
                    "error", "Please select a valid class.");
            return "redirect:/teacher/students";
        }

        String password = newPassword();

        Student student = new Student(
                UUID.randomUUID().toString(),
                name,
                username,
                encoder.encode(password),
                classPeriod
        );

        students.save(student);

        redirect.addFlashAttribute(
                "success", "Student account created.");
        redirect.addFlashAttribute(
                "credentials",
                List.of(username + " : " + password));

        return "redirect:/teacher/students";
    }

    @PostMapping("/{id}/move")
    public String move(
            @PathVariable String id,
            @RequestParam Long classId,
            RedirectAttributes redirect) {

        var student = students.findById(id).orElse(null);
        var classPeriod = classes.findById(classId).orElse(null);

        if (student == null || student.teacher ||
                classPeriod == null) {
            redirect.addFlashAttribute(
                    "error", "Student or class not found.");
            return "redirect:/teacher/students";
        }

        student.classPeriod = classPeriod;
        students.save(student);

        redirect.addFlashAttribute(
                "success", "Student moved successfully.");

        return "redirect:/teacher/students";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable String id,
            RedirectAttributes redirect) {

        var student = students.findById(id).orElse(null);

        if (student == null || student.teacher) {
            redirect.addFlashAttribute(
                    "error", "Student not found.");
            return "redirect:/teacher/students";
        }

        String password = newPassword();

        student.passwordHash = encoder.encode(password);
        student.mustChangePassword = true;
        students.save(student);

        redirect.addFlashAttribute(
                "success", "Password reset.");
        redirect.addFlashAttribute(
                "credentials",
                List.of(student.username + " : " + password));

        return "redirect:/teacher/students";
    }

    @PostMapping("/import")
    public String importCsv(
            @RequestParam Long classId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirect) throws Exception {

        var classPeriod = classes.findById(classId).orElse(null);

        if (classPeriod == null) {
            redirect.addFlashAttribute(
                    "error", "Invalid class period.");
            return "redirect:/teacher/students";
        }

        if (file.isEmpty() || file.getSize() > 1_000_000) {
            redirect.addFlashAttribute(
                    "error", "CSV must be under 1 MB.");
            return "redirect:/teacher/students";
        }

        String csv = new String(
                file.getBytes(), StandardCharsets.UTF_8);

        List<String> credentials = new ArrayList<>();
        int imported = 0;

        String[] lines = csv.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;

            String[] columns = line.split(",", -1);
            if (columns.length != 2) continue;

            String name = columns[0].trim();
            String username = columns[1].trim()
                    .toLowerCase(Locale.ROOT);

            if (i == 0 && name.equalsIgnoreCase("name")
                    && username.equalsIgnoreCase("username")) {
                continue;
            }

            if (name.isBlank() || name.length() > 120 ||
                    !username.matches("[a-z0-9._-]{3,40}") ||
                    username.equals("teacher") ||
                    students.findByUsername(username).isPresent()) {
                continue;
            }

            String password = newPassword();

            Student student = new Student(
                    UUID.randomUUID().toString(),
                    name,
                    username,
                    encoder.encode(password),
                    classPeriod
            );

            students.save(student);

            credentials.add(username + " : " + password);
            imported++;
        }

        redirect.addFlashAttribute(
                "success", imported + " students imported.");
        redirect.addFlashAttribute(
                "credentials", credentials);

        return "redirect:/teacher/students";
    }

    private String newPassword() {

        String[] firstWords = {
                "blue", "green", "happy", "super",
                "tiny", "fast", "cool", "red",
                "purple", "silver", "sunny", "brave",
                "magic", "orange", "yellow", "fuzzy",
                "loud", "magic", "angry", "orange"
        };

        String[] secondWords = {
                "tiger", "pizza", "robot", "panda",
                "apple", "frog", "dragon", "turtle",
                "monkey", "cookie", "rocket", "penguin",
                "banana", "shark", "potato", "dolphin",
                "aura", "six7", "bluey", "fish",
                "grannies", "rock"
        };

        String first = firstWords[
                random.nextInt(firstWords.length)
                ];

        String second = secondWords[
                random.nextInt(secondWords.length)
                ];

        int number = 10 + random.nextInt(90);

        return first + second + number;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteStudent(
            @PathVariable String id,
            RedirectAttributes redirect) {

        Student student = students.findById(id).orElse(null);

        if (student == null) {
            redirect.addFlashAttribute(
                    "error", "Student not found.");
            return "redirect:/teacher/students";
        }

        if (student.teacher) {
            redirect.addFlashAttribute(
                    "error", "Teacher accounts cannot be deleted here.");
            return "redirect:/teacher/students";
        }

        String studentName = student.name;
        String username = student.username;

        // Delete submitted assignments and grades.
        long deletedSubmissions =
                submissions.deleteByStudentId(id);

        // Delete saved assignment drafts.
        long deletedDrafts =
                drafts.deleteByStudent_Id(id);

        // Sandbox programs use username, not student UUID.
        long deletedSandboxes =
                sandboxes.deleteByStudentId(username);

        // Flush dependent record deletions before deleting student.
        submissions.flush();
        drafts.flush();
        sandboxes.flush();

        // Finally delete the student account.
        students.delete(student);
        students.flush();

        redirect.addFlashAttribute(
                "success",
                studentName + " was deleted successfully. " +
                        deletedSubmissions + " submissions, " +
                        deletedDrafts + " drafts, and " +
                        deletedSandboxes + " sandbox programs removed."
        );

        return "redirect:/teacher/students";
    }
}
