
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.ClassPeriod;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.models.Submission;
import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.ClassPeriods;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Submissions;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher/gradebook")
public class GradebookController {

    private final ClassPeriods classes;
    private final Students students;
    private final Assignments assignments;
    private final Submissions submissions;

    public GradebookController(
            ClassPeriods classes,
            Students students,
            Assignments assignments,
            Submissions submissions) {

        this.classes = classes;
        this.students = students;
        this.assignments = assignments;
        this.submissions = submissions;
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

    @GetMapping
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
            classStudents = students.findByClassPeriodId(selectedClassId);
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

        // Each cell uses the newest submission from that student.
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

        // Track whether each student has ever had a graded
        // submission for an assignment.
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

                // For a pending resubmission, retain the last
                // graded score in the display.
                if ("resubmitted".equals(status)) {
                    score = allSubmissions.stream()
                            .filter(s -> s.student.id.equals(student.id))
                            .filter(s -> s.assignment.id.equals(assignment.id))
                            .filter(s -> s.grade != null)
                            .max(Comparator
                                    .comparing((Submission s) ->
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

        model.addAttribute("classes", allClasses);
        model.addAttribute("allClasses", allClasses);
        model.addAttribute("selectedClassId", selectedClassId);
        model.addAttribute("assignments", classAssignments);
        model.addAttribute("rows", rows);


        return "gradebook";
    }


    @PostMapping("/assignments/create")
    @Transactional
    public String createAssignment(
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "") String starterCode,
            @RequestParam(defaultValue = "100") int maxPoints,
            @RequestParam(required = false) List<Long> classIds,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = "redirect:/teacher/gradebook";

        if (returnClassId != null) {
            destination += "?classId=" + returnClassId;
        }

        if (title == null || title.isBlank()
                || title.trim().length() > 160
                || description.length() > 12000
                || starterCode.length() > 12000
                || maxPoints < 1 || maxPoints > 10000) {

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

        Assignment assignment = new Assignment();

        assignment.title = title.trim();
        assignment.description = description;
        assignment.starterCode = starterCode;
        assignment.maxPoints = maxPoints;
        assignment.classPeriods = new HashSet<>(selectedClasses);

        assignments.save(assignment);

        redirect.addFlashAttribute(
                "success", "Assignment created successfully!");

        return destination;
    }

    @PostMapping("/assignments/{id}/edit")
    @Transactional
    public String editAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam(defaultValue = "") String starterCode,
            @RequestParam int maxPoints,
            @RequestParam(required = false) List<Long> classIds,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = gradebookRedirect(returnClassId);

        Assignment assignment = assignments.findById(id).orElse(null);

        if (assignment == null) {
            redirect.addFlashAttribute("error", "Assignment not found.");
            return destination;
        }

        if (title == null || title.isBlank()
                || title.trim().length() > 160
                || description.length() > 12000
                || starterCode.length() > 12000
                || maxPoints < 1 || maxPoints > 10000) {

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
        List<ClassPeriod> selectedClasses = classes.findAllById(uniqueIds);

        if (selectedClasses.size() != uniqueIds.size()) {
            redirect.addFlashAttribute(
                    "error", "One or more class periods do not exist.");
            return destination;
        }

        // Protect existing grades from accidental changes to the
        // maximum possible score.
        if (assignment.maxPoints != maxPoints
                && submissions.existsByAssignmentId(id)) {

            redirect.addFlashAttribute(
                    "error",
                    "Cannot change maximum points after students " +
                            "have submitted work.");
            return destination;
        }

        assignment.title = title.trim();
        assignment.description = description;
        assignment.starterCode = starterCode;
        assignment.maxPoints = maxPoints;
        assignment.classPeriods = new HashSet<>(selectedClasses);

        assignments.save(assignment);

        redirect.addFlashAttribute(
                "success", "Assignment updated successfully!");

        return destination;
    }

    @PostMapping("/assignments/{id}/delete")
    @Transactional
    public String deleteAssignment(
            @PathVariable Long id,
            @RequestParam(required = false) Long returnClassId,
            RedirectAttributes redirect) {

        String destination = gradebookRedirect(returnClassId);

        Assignment assignment = assignments.findById(id).orElse(null);

        if (assignment == null) {
            redirect.addFlashAttribute("error", "Assignment not found.");
            return destination;
        }

        if (submissions.existsByAssignmentId(id)) {
            redirect.addFlashAttribute(
                    "error",
                    "This assignment already has student submissions. " +
                            "Deletion is blocked to protect student work.");
            return destination;
        }

        assignment.classPeriods.clear();
        assignments.delete(assignment);

        redirect.addFlashAttribute(
                "success", "Assignment deleted successfully.");

        return destination;
    }

    private String gradebookRedirect(Long classId) {
        return classId == null
                ? "redirect:/teacher/gradebook"
                : "redirect:/teacher/gradebook?classId=" + classId;
    }


}
