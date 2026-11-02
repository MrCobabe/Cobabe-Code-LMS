
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
import com.laf.cobabe_code_lms.models.AssignmentDraft;
import com.laf.cobabe_code_lms.models.AssignmentDraftSourceFile;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import com.laf.cobabe_code_lms.repositories.AssignmentDraftSourceFiles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.time.Instant;
import java.util.Objects;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Assignments;

@Controller
@RequestMapping("/teacher/submission")
public class TeacherSubmissionReviewController {

    private final Submissions submissions;
    private final JavaExecutionService executionService;
    private final SubmissionSourceFiles submissionSourceFiles;
    private final AssignmentDrafts assignmentDrafts;
    private final AssignmentDraftSourceFiles assignmentDraftSourceFiles;
    private final Students students;
    private final Assignments assignments;

    public TeacherSubmissionReviewController(
            Submissions submissions,
            JavaExecutionService executionService, SubmissionSourceFiles submissionSourceFiles, AssignmentDrafts assignmentDrafts, AssignmentDraftSourceFiles assignmentDraftSourceFiles, Students students, Assignments assignments) {

        this.submissions = submissions;
        this.executionService = executionService;
        this.submissionSourceFiles = submissionSourceFiles;
        this.assignmentDrafts = assignmentDrafts;
        this.assignmentDraftSourceFiles = assignmentDraftSourceFiles;
        this.students = students;
        this.assignments = assignments;
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
        model.addAttribute("assignment", submission.assignment);
        model.addAttribute("sourceFiles", sourceFiles);

        return "review";
    }

    @GetMapping("/draft/{studentId}/{assignmentId}")
    @Transactional(readOnly = true)
    public String reviewDraft(
            @PathVariable String studentId,
            @PathVariable Long assignmentId,
            Authentication authentication,
            Model model) {

        requireTeacher(authentication);

        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        // Verify the student belongs to a class assigned this work.
        if (student.classPeriod == null ||
                assignment.classPeriods.stream().noneMatch(period ->
                        Objects.equals(period.id, student.classPeriod.id))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Student is not assigned this work");
        }

        AssignmentDraft draft = assignmentDrafts
                .findByStudent_IdAndAssignment_Id(studentId, assignmentId)
                .orElse(null);

        Map<String, String> sourceFiles = new LinkedHashMap<>();

        if (draft != null) {

            // Show the student's actual saved work.
            sourceFiles.put("Main.java",
                    draft.code == null ? "" : draft.code);

            for (AssignmentDraftSourceFile file :
                    assignmentDraftSourceFiles
                            .findByDraft_IdOrderByFilenameAsc(draft.id)) {

                if (!"Main.java".equals(file.getFilename())) {
                    sourceFiles.put(file.getFilename(), file.getCode());
                }
            }

        } else {

            // Student hasn't started: show the original starter code.
            sourceFiles.put("Main.java",
                    assignment.starterCode == null ? "" : assignment.starterCode);
        }

        model.addAttribute("student", student);
        model.addAttribute("assignment", assignment);
        model.addAttribute("draft", draft);
        model.addAttribute("draftMode", true);
        model.addAttribute("notStarted", draft == null);
        model.addAttribute("sourceFiles", sourceFiles);

        return "review";
    }


    @PostMapping("/draft/{studentId}/{assignmentId}/grade")
    @Transactional
    public String gradeDraft(
            @PathVariable String studentId,
            @PathVariable Long assignmentId,
            @RequestParam int grade,
            @RequestParam(defaultValue = "") String feedback,
            Authentication authentication,
            RedirectAttributes redirect) {

        requireTeacher(authentication);

        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));

        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        // Ensure this assignment belongs to the student's class.
        if (student.classPeriod == null ||
                assignment.classPeriods.stream().noneMatch(period ->
                        Objects.equals(period.id, student.classPeriod.id))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Student is not assigned this work");
        }

        String draftUrl = "/teacher/submission/draft/"
                + studentId + "/" + assignmentId;

        // Validate the grade.
        if (grade < 0 || grade > assignment.maxPoints) {
            redirect.addFlashAttribute(
                    "error",
                    "Grade must be between 0 and " + assignment.maxPoints);
            return "redirect:" + draftUrl;
        }

        if (feedback.length() > 4000) {
            redirect.addFlashAttribute(
                    "error",
                    "Feedback must be 4000 characters or fewer.");
            return "redirect:" + draftUrl;
        }

        // Retrieve the student's saved draft, if one exists.
        AssignmentDraft draft = assignmentDrafts
                .findByStudent_IdAndAssignment_Id(studentId, assignmentId)
                .orElse(null);

        // Create a graded submission snapshot.
        Submission submission = new Submission();
        submission.student = student;
        submission.assignment = assignment;

        if (draft != null) {
            submission.code = draft.code == null ? "" : draft.code;
        } else {
            // No student work exists. Record an empty submission,
            // not the teacher's starter code.
            submission.code = "";
        }

        submission.submittedAt = Instant.now();
        submission.grade = grade;
        submission.feedback = feedback;

        submission = submissions.saveAndFlush(submission);

        // Preserve additional Java files when grading a saved draft.
        if (draft != null) {
            for (AssignmentDraftSourceFile file :
                    assignmentDraftSourceFiles
                            .findByDraft_IdOrderByFilenameAsc(draft.id)) {

                if ("Main.java".equals(file.getFilename())) {
                    continue;
                }

                submissionSourceFiles.save(
                        new SubmissionSourceFile(
                                submission,
                                file.getFilename(),
                                file.getCode()
                        )
                );
            }
        }

        redirect.addFlashAttribute(
                "success",
                "Grade saved successfully!");

        return "redirect:/teacher/gradebook?classId="
                + student.classPeriod.id;
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
