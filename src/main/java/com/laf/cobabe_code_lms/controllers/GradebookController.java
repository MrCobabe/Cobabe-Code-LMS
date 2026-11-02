
package com.laf.cobabe_code_lms.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.laf.cobabe_code_lms.models.*;

import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.ClassPeriods;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Submissions;
import com.laf.cobabe_code_lms.repositories.AssignmentDraftSourceFiles;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher/gradebook")
public class GradebookController {

    private final ClassPeriods classes;
    private final Students students;
    private final Assignments assignments;
    private final Submissions submissions;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssignmentDrafts assignmentDrafts;
    private final AssignmentDraftSourceFiles draftSourceFiles;

    public GradebookController(
            ClassPeriods classes,
            Students students,
            Assignments assignments,
            Submissions submissions, AssignmentDrafts assignmentDrafts, AssignmentDraftSourceFiles draftSourceFiles) {

        this.classes = classes;
        this.students = students;
        this.assignments = assignments;
        this.submissions = submissions;
        this.assignmentDrafts = assignmentDrafts;
        this.draftSourceFiles = draftSourceFiles;
    }

    public record GradeCell(
            Long assignmentId,
            Long submissionId,
            String status,
            Integer score,
            int maxPoints
    ) {}

    public record StudentRow(
            String studentId,
            String name,
            List<GradeCell> cells,
            String average
    ) {}

    // ==========================================
    // GRADEBOOK
    // ==========================================

    @GetMapping
    @Transactional(readOnly = true)
    public String gradebook(
            @RequestParam(required = false) Long classId,
            Model model) {

        List<ClassPeriod> allClasses = classes.findAll();

        allClasses.sort(
                Comparator.comparingInt(c -> c.periodNumber));

        Long selectedClassId = classId;

        if (selectedClassId == null && !allClasses.isEmpty()) {
            selectedClassId = allClasses.get(0).id;
        }

        if (selectedClassId != null
                && classes.findById(selectedClassId).isEmpty()) {
            return "redirect:/teacher/gradebook";
        }

        List<Student> classStudents;
        List<Assignment> classAssignments;

        if (selectedClassId == null) {
            classStudents = List.of();
            classAssignments = List.of();
        } else {
            classStudents = students.findByClassPeriodId(
                    selectedClassId);

            classAssignments =
                    assignments.findDistinctByClassPeriods_Id(
                            selectedClassId);
        }

        classStudents.sort(
                Comparator.comparing(
                        s -> s.name == null
                                ? ""
                                : s.name
                                .replace('\u00A0', ' ')
                                .replace("\uFEFF", "")
                                .strip(),
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        classAssignments.sort(
                Comparator.comparing(a -> a.id));

        Set<String> studentIds = classStudents.stream()
                .map(s -> s.id)
                .collect(Collectors.toSet());

        List<Long> assignmentIds = classAssignments.stream()
                .map(a -> a.id)
                .toList();

        List<Submission> allSubmissions = assignmentIds.isEmpty()
                ? List.of()
                : submissions.findByAssignmentIdIn(assignmentIds);

        // Each cell uses the newest submission.
        Map<String, Submission> latest = new HashMap<>();

        for (Submission submission : allSubmissions) {

            if (!studentIds.contains(submission.student.id)) {
                continue;
            }

            String key = submission.student.id
                    + ":" + submission.assignment.id;

            Submission previous = latest.get(key);

            if (previous == null
                    || submission.submittedAt.isAfter(
                    previous.submittedAt)
                    || (submission.submittedAt.equals(
                    previous.submittedAt)
                    && submission.id > previous.id)) {

                latest.put(key, submission);
            }
        }

        // Track previously graded submissions.
        Set<String> previouslyGraded = new HashSet<>();

        for (Submission submission : allSubmissions) {
            if (submission.grade != null) {
                previouslyGraded.add(
                        submission.student.id + ":"
                                + submission.assignment.id);
            }
        }

        List<StudentRow> rows = new ArrayList<>();

        for (Student student : classStudents) {

            List<GradeCell> cells = new ArrayList<>();
            int earned = 0;
            int possible = 0;

            for (Assignment assignment : classAssignments) {

                String key = student.id + ":" + assignment.id;
                Submission newest = latest.get(key);

                String status;
                Integer score = null;

                if (newest == null) {
                    status = "missing";

                } else if (newest.grade != null) {
                    status = "graded";
                    score = newest.grade;

                } else if (previouslyGraded.contains(key)) {
                    status = "resubmitted";

                } else {
                    status = "submitted";
                }

                // Preserve the last graded score when
                // a student has resubmitted work.
                if ("resubmitted".equals(status)) {

                    score = allSubmissions.stream()
                            .filter(s -> s.student.id.equals(
                                    student.id))
                            .filter(s -> s.assignment.id.equals(
                                    assignment.id))
                            .filter(s -> s.grade != null)
                            .max(Comparator
                                    .comparing(
                                            (Submission s) ->
                                                    s.submittedAt)
                                    .thenComparing(s -> s.id))
                            .map(s -> s.grade)
                            .orElse(null);
                }

                if (score != null && assignment.maxPoints > 0) {
                    earned += score;
                    possible += assignment.maxPoints;
                }

                cells.add(new GradeCell(
                        assignment.id,
                        newest == null ? null : newest.id,
                        status,
                        score,
                        assignment.maxPoints));
            }

            String average = possible == 0
                    ? "—"
                    : String.format(
                    Locale.US,
                    "%.1f%%",
                    earned * 100.0 / possible);

            rows.add(new StudentRow(
                    student.id,
                    student.name,
                    cells,
                    average));
        }

        // Provide starter files to the assignment editor.
        Map<Long, Map<String, String>> starterFilesByAssignment =
                new HashMap<>();

        for (Assignment assignment : classAssignments) {

            Map<String, String> files = new LinkedHashMap<>();

            files.put(
                    "Main.java",
                    assignment.starterCode == null
                            ? ""
                            : assignment.starterCode);

            if (assignment.starterFiles != null) {
                files.putAll(assignment.starterFiles);
            }

            starterFilesByAssignment.put(assignment.id, files);
        }

        model.addAttribute("classes", allClasses);
        model.addAttribute("allClasses", allClasses);
        model.addAttribute("selectedClassId", selectedClassId);
        model.addAttribute("assignments", classAssignments);
        model.addAttribute("rows", rows);

        model.addAttribute(
                "starterFilesByAssignment",
                starterFilesByAssignment);

        return "gradebook";
    }

    // ==========================================
    // CREATE ASSIGNMENT
    // ==========================================

    @PostMapping("/assignments/create")
    @Transactional
    public String createAssignment(
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "") String starterCode,
            @RequestParam(required = false) String starterFilesJson,
            @RequestParam(defaultValue = "100") int maxPoints,
            @RequestParam(required = false) List<Long> classIds,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = gradebookRedirect(returnClassId);

        if (!validAssignmentDetails(
                title, description, starterCode, maxPoints)) {

            redirect.addFlashAttribute(
                    "error", "Please check the assignment details.");

            return destination;
        }

        if (classIds == null || classIds.isEmpty()) {
            redirect.addFlashAttribute(
                    "error", "Select at least one class period.");

            return destination;
        }

        Set<Long> uniqueIds = new HashSet<>(classIds);

        List<ClassPeriod> selectedClasses =
                classes.findAllById(uniqueIds);

        if (selectedClasses.size() != uniqueIds.size()) {
            redirect.addFlashAttribute(
                    "error", "One or more selected classes do not exist.");

            return destination;
        }

        Map<String, String> additionalFiles;

        try {
            additionalFiles = parseStarterFiles(
                    starterFilesJson);

        } catch (IllegalArgumentException ex) {

            redirect.addFlashAttribute(
                    "error", ex.getMessage());

            return destination;
        }

        Assignment assignment = new Assignment();

        assignment.title = title.trim();
        assignment.description = description;
        assignment.starterCode = starterCode;
        assignment.maxPoints = maxPoints;
        assignment.classPeriods = new HashSet<>(selectedClasses);

        assignment.starterFiles.putAll(additionalFiles);

        assignments.save(assignment);

        redirect.addFlashAttribute(
                "success", "Assignment created successfully!");

        return destination;
    }

    // ==========================================
    // EDIT ASSIGNMENT
    // ==========================================

    @PostMapping("/assignments/{id}/edit")
    @Transactional
    public String editAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "") String starterCode,
            @RequestParam(required = false) String starterFilesJson,
            @RequestParam int maxPoints,
            @RequestParam(required = false) List<Long> classIds,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = gradebookRedirect(returnClassId);

        Assignment assignment =
                assignments.findById(id).orElse(null);

        if (assignment == null) {
            redirect.addFlashAttribute(
                    "error", "Assignment not found.");

            return destination;
        }

        if (!validAssignmentDetails(
                title, description, starterCode, maxPoints)) {

            redirect.addFlashAttribute(
                    "error", "Please check the assignment details.");

            return destination;
        }

        if (classIds == null || classIds.isEmpty()) {
            redirect.addFlashAttribute(
                    "error", "Select at least one class period.");

            return destination;
        }

        Set<Long> uniqueIds = new HashSet<>(classIds);

        List<ClassPeriod> selectedClasses =
                classes.findAllById(uniqueIds);

        if (selectedClasses.size() != uniqueIds.size()) {
            redirect.addFlashAttribute(
                    "error", "One or more class periods do not exist.");

            return destination;
        }

        // Protect grades when students have submitted.
        if (assignment.maxPoints != maxPoints
                && submissions.existsByAssignmentId(id)) {

            redirect.addFlashAttribute(
                    "error",
                    "Cannot change maximum points after students "
                            + "have submitted work.");

            return destination;
        }

        Map<String, String> additionalFiles = null;

        // Null means the old form was used.
        // Preserve existing additional files in that case.
        if (starterFilesJson != null) {

            try {
                additionalFiles = parseStarterFiles(
                        starterFilesJson);

            } catch (IllegalArgumentException ex) {

                redirect.addFlashAttribute(
                        "error", ex.getMessage());

                return destination;
            }
        }

        assignment.title = title.trim();
        assignment.description = description;
        assignment.starterCode = starterCode;
        assignment.maxPoints = maxPoints;
        assignment.classPeriods = new HashSet<>(selectedClasses);

        if (additionalFiles != null) {
            assignment.starterFiles.clear();
            assignment.starterFiles.putAll(additionalFiles);
        }

        assignments.save(assignment);

        redirect.addFlashAttribute(
                "success", "Assignment updated successfully!");

        return destination;
    }

    // ==========================================
    // DELETE ASSIGNMENT
    // ==========================================



    @PostMapping("/assignments/{id}/delete")
    @Transactional
    public String deleteAssignment(
            @PathVariable Long id,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = gradebookRedirect(returnClassId);

        Assignment assignment =
                assignments.findById(id).orElse(null);

        if (assignment == null) {
            redirect.addFlashAttribute(
                    "error", "Assignment not found.");

            return destination;
        }

        // Protect submitted student work and grades.
        if (submissions.existsByAssignmentId(id)) {
            redirect.addFlashAttribute(
                    "error",
                    "This assignment already has student submissions. "
                            + "Deletion is blocked to protect student work.");

            return destination;
        }

        // Find all drafts associated with this assignment.
        List<AssignmentDraft> assignmentDraftList =
                assignmentDrafts.findByAssignment_Id(id);

        // Delete additional Java source files first.
        for (AssignmentDraft draft : assignmentDraftList) {
            draftSourceFiles.deleteByDraft_Id(draft.id);
        }

        // Ensure source file deletions are completed.
        draftSourceFiles.flush();

        // Now delete the parent draft records.
        assignmentDrafts.deleteByAssignment_Id(id);
        assignmentDrafts.flush();

        // Remove the assignment's class period associations.
        assignment.classPeriods.clear();

        // Finally, delete the assignment itself.
        assignments.delete(assignment);

        redirect.addFlashAttribute(
                "success",
                "Assignment deleted successfully.");

        return destination;
    }



    // ==========================================
    // VALIDATION
    // ==========================================

    private boolean validAssignmentDetails(
            String title,
            String description,
            String starterCode,
            int maxPoints) {

        return title != null
                && !title.isBlank()
                && title.trim().length() <= 160
                && description != null
                && description.length() <= 12000
                && starterCode != null
                && starterCode.length() <= 12000
                && maxPoints >= 1
                && maxPoints <= 10000;
    }

    private Map<String, String> parseStarterFiles(
            String json) {

        Map<String, String> result = new LinkedHashMap<>();

        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            Map<String, String> files =
                    objectMapper.readValue(
                            json,
                            new TypeReference<Map<String, String>>() {}
                    );

            if (files == null) {
                throw new IllegalArgumentException(
                        "Invalid starter file data.");
            }

            // 20 total files, including Main.java.
            if (files.size() > 20) {
                throw new IllegalArgumentException(
                        "An assignment can contain at most 20 Java files.");
            }

            for (Map.Entry<String, String> entry :
                    files.entrySet()) {

                String filename = entry.getKey();
                String code = entry.getValue();

                if (filename == null
                        || !filename.matches(
                        "[A-Za-z_$][A-Za-z0-9_$]*\\.java")
                        || filename.length() > 120) {

                    throw new IllegalArgumentException(
                            "Invalid Java filename: " + filename);
                }

                if ("Main.java".equals(filename)) {
                    continue;
                }

                if (code == null || code.length() > 12000) {
                    throw new IllegalArgumentException(
                            "Starter code is too long or missing for "
                                    + filename);
                }

                result.put(filename, code);
            }

            if (result.size() > 19) {
                throw new IllegalArgumentException(
                        "An assignment can contain at most 20 Java files.");
            }

            return result;

        } catch (IllegalArgumentException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Could not read the Java starter files.");
        }
    }

    // ==========================================
    // REDIRECT
    // ==========================================

    private String gradebookRedirect(Long classId) {
        return classId == null
                ? "redirect:/teacher/gradebook"
                : "redirect:/teacher/gradebook?classId=" + classId;
    }
}
