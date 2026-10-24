package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.models.Submission;

import com.laf.cobabe_code_lms.models.SubmissionSourceFile;
import com.laf.cobabe_code_lms.repositories.SubmissionSourceFiles;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Submissions;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Objects;

@Controller
public class AssignmentSubmissionController {

    private final Assignments assignments;
    private final Students students;
    private final Submissions submissions;
    private final SubmissionSourceFiles submissionSourceFiles;
    private final ObjectMapper objectMapper;

    public AssignmentSubmissionController(
            Assignments assignments,
            Students students,
            Submissions submissions, SubmissionSourceFiles submissionSourceFiles, ObjectMapper objectMapper) {

        this.assignments = assignments;
        this.students = students;
        this.submissions = submissions;
        this.submissionSourceFiles = submissionSourceFiles;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/assignment/{id}/submit")
    @Transactional
    public String submit(
            @PathVariable Long id,
            @RequestParam("code") String code,
            @RequestParam(value = "filesJson", required = false)
            String filesJson,
            Authentication authentication,
            RedirectAttributes redirect) {


        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED);
        }

        Student student = students
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN));

        if (student.teacher || student.classPeriod == null ||
                student.classPeriod.id == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN);
        }

        Assignment assignment = assignments.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        boolean allowed = assignment.classPeriods.stream()
                .anyMatch(period ->
                        Objects.equals(
                                period.id,
                                student.classPeriod.id));

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }


        Map<String, String> files = new java.util.LinkedHashMap<>();

        if (filesJson != null && !filesJson.isBlank()) {
            try {
                files = objectMapper.readValue(
                        filesJson,
                        new TypeReference<Map<String, String>>() {}
                );
            } catch (Exception e) {
                redirect.addFlashAttribute(
                        "error", "Invalid Java files submitted.");
                return "redirect:/student/assignment/" + id;
            }
        } else {
            // Original single-file submissions still work.
            files.put("Main.java", code);
        }

        if (files == null
                || !files.containsKey("Main.java")
                || files.get("Main.java") == null) {
            redirect.addFlashAttribute(
                    "error", "Main.java is required.");
            return "redirect:/student/assignment/" + id;
        }

        if (files.size() > 20) {
            redirect.addFlashAttribute(
                    "error", "Maximum of 20 Java files allowed.");
            return "redirect:/student/assignment/" + id;
        }

        long totalBytes = 0;
        boolean hasCode = false;

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String filename = entry.getKey();
            String source = entry.getValue();

            if (filename == null
                    || filename.length() > 120
                    || !filename.matches(
                    "[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {
                redirect.addFlashAttribute(
                        "error", "Invalid Java filename.");
                return "redirect:/student/assignment/" + id;
            }

            if (source == null) {
                redirect.addFlashAttribute(
                        "error", "Java file contents cannot be null.");
                return "redirect:/student/assignment/" + id;
            }

            if (!source.isBlank()) {
                hasCode = true;
            }

            totalBytes += source.getBytes(
                    StandardCharsets.UTF_8
            ).length;

            if (totalBytes > 65536) {
                redirect.addFlashAttribute(
                        "error", "Total Java source exceeds 64 KB.");
                return "redirect:/student/assignment/" + id;
            }
        }

        if (!hasCode) {
            redirect.addFlashAttribute(
                    "error", "Please write some code before submitting.");
            return "redirect:/student/assignment/" + id;
        }

        Submission submission = new Submission();

        submission.assignment = assignment;
        submission.student = student;
        submission.code = files.get("Main.java");
        submission.submittedAt = Instant.now();
        submission.grade = null;
        submission.feedback = null;

        submission = submissions.saveAndFlush(submission);

        for (Map.Entry<String, String> entry : files.entrySet()) {
            if ("Main.java".equals(entry.getKey())) {
                continue;
            }

            SubmissionSourceFile sourceFile =
                    new SubmissionSourceFile(
                            submission,
                            entry.getKey(),
                            entry.getValue()
                    );

            submissionSourceFiles.save(sourceFile);
        }

        redirect.addFlashAttribute(
                "success",
                "Assignment submitted successfully!");

        return "redirect:/student";
    }

}