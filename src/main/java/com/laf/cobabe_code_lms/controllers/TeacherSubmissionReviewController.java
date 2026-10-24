
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Submission;
import com.laf.cobabe_code_lms.repositories.Submissions;
import com.laf.cobabe_code_lms.services.JavaExecutionService;
import com.laf.cobabe_code_lms.models.SubmissionSourceFile;
import com.laf.cobabe_code_lms.repositories.SubmissionSourceFiles;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

@Controller
@RequestMapping("/teacher/submission")
public class TeacherSubmissionReviewController {

    private final Submissions submissions;
    private final JavaExecutionService executionService;
    private final SubmissionSourceFiles submissionSourceFiles;

    public TeacherSubmissionReviewController(
            Submissions submissions,
            JavaExecutionService executionService, SubmissionSourceFiles submissionSourceFiles) {

        this.submissions = submissions;
        this.executionService = executionService;
        this.submissionSourceFiles = submissionSourceFiles;
    }

    private void requireTeacher(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getAuthorities().stream()
                        .noneMatch(a ->
                                a.getAuthority().equals("ROLE_TEACHER"))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Teacher access required");
        }
    }


    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String review(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        requireTeacher(authentication);

        Submission submission = submissions.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Submission not found"));

        // Collect all submitted Java files.
        Map<String, String> sourceFiles = new LinkedHashMap<>();

        // Main.java remains in the original Submission record.
        sourceFiles.put("Main.java", submission.code);

        // Retrieve additional files from the new table.
        for (SubmissionSourceFile sourceFile :
                submissionSourceFiles
                        .findBySubmission_IdOrderByFilenameAsc(id)) {

            sourceFiles.put(
                    sourceFile.getFilename(),
                    sourceFile.getCode()
            );
        }

        model.addAttribute("submission", submission);
        model.addAttribute("sourceFiles", sourceFiles);

        return "review";
    }


    @PostMapping("/{id}/grade")
    @Transactional
    public String saveGrade(
            @PathVariable Long id,
            @RequestParam int grade,
            @RequestParam(defaultValue = "") String feedback,
            Authentication authentication,
            RedirectAttributes redirect) {

        requireTeacher(authentication);

        Submission submission = submissions.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        int maxPoints = submission.assignment.maxPoints;

        if (grade < 0 || grade > maxPoints) {

            redirect.addFlashAttribute(
                    "error",
                    "Grade must be between 0 and " + maxPoints);

            return "redirect:/teacher/submission/" + id;
        }

        if (feedback.length() > 4000) {

            redirect.addFlashAttribute(
                    "error",
                    "Feedback must be 4000 characters or fewer.");

            return "redirect:/teacher/submission/" + id;
        }

        submission.grade = grade;
        submission.feedback = feedback;

        submissions.save(submission);

        redirect.addFlashAttribute(
                "success",
                "Grade saved successfully!");

        Long classId = submission.student.classPeriod == null
                ? null
                : submission.student.classPeriod.id;

        if (classId == null) {
            return "redirect:/teacher/gradebook";
        }

        return "redirect:/teacher/gradebook?classId=" + classId;
    }

    public record RunRequest(String code, String input) {}

    @PostMapping("/run")
    @ResponseBody
    public CompletableFuture<ResponseEntity<
            JavaExecutionService.ExecutionResult>> runCode(
            @RequestBody RunRequest request,
            Authentication authentication) {

        requireTeacher(authentication);

        if (request == null ||
                request.code() == null ||
                request.code().isBlank() ||
                request.code().length() > 50000) {

            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build());
        }

        return executionService.testRunnerConnection(
                        request.code(),
                        request.input() == null ? "" : request.input())
                .handle((result, exception) -> {

                    if (exception != null) {

                        Throwable cause = exception;

                        while (cause instanceof CompletionException
                                && cause.getCause() != null) {
                            cause = cause.getCause();
                        }

                        if (cause instanceof RejectedExecutionException) {

                            return ResponseEntity.status(429)
                                    .body(
                                            new JavaExecutionService.ExecutionResult(
                                                    false,
                                                    "",
                                                    "Runner busy. Try again shortly."
                                            ));
                        }

                        return ResponseEntity.status(500)
                                .body(
                                        new JavaExecutionService.ExecutionResult(
                                                false,
                                                "",
                                                "Java runner encountered an error."
                                        ));
                    }

                    return ResponseEntity.ok(result);
                });
    }
}
