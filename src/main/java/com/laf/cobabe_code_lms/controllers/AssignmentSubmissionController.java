package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.models.Submission;

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

    public AssignmentSubmissionController(
            Assignments assignments,
            Students students,
            Submissions submissions) {

        this.assignments = assignments;
        this.students = students;
        this.submissions = submissions;
    }

    @PostMapping("/assignment/{id}/submit")
    @Transactional
    public String submit(
            @PathVariable Long id,
            @RequestParam("code") String code,
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

        if (code == null || code.isBlank()) {
            redirect.addFlashAttribute(
                    "error",
                    "Please write some code before submitting.");

            return "redirect:/student/assignment/" + id;
        }

        if (code.length() > 50000) {
            redirect.addFlashAttribute(
                    "error",
                    "Code is too large to submit.");

            return "redirect:/student/assignment/" + id;
        }

        Submission submission = new Submission();

        submission.assignment = assignment;
        submission.student = student;
        submission.code = code;
        submission.submittedAt = Instant.now();
        submission.grade = null;
        submission.feedback = null;

        submissions.save(submission);

        redirect.addFlashAttribute(
                "success",
                "Assignment submitted successfully!");

        return "redirect:/student";
    }
}