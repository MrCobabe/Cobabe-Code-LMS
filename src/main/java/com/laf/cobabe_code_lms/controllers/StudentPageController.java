
package com.laf.cobabe_code_lms.controllers;

import java.security.Principal;
import java.util.List;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.Students;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.laf.cobabe_code_lms.models.Submission;
import com.laf.cobabe_code_lms.repositories.Submissions;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Controller
public class StudentPageController {

    private final Students students;
    private final Assignments assignments;
    private final Submissions submissions;

    public StudentPageController(
            Students students,
            Assignments assignments, Submissions submissions) {

        this.students = students;
        this.assignments = assignments;
        this.submissions = submissions;
    }

    @GetMapping("/student")
    public String dashboard(
            Principal principal,
            Model model) {

        Student student = currentStudent(principal);

        model.addAttribute("studentName", student.name);

        if (student.classPeriod == null) {

            model.addAttribute(
                    "className", "No class assigned");

            model.addAttribute("assignments", List.of());
            model.addAttribute("assignmentStatuses", Map.of());

        } else {

            model.addAttribute(
                    "className",
                    student.classPeriod.name +
                            " — Period " +
                            student.classPeriod.periodNumber);

            List<Assignment> studentAssignments =
                    assignments.findDistinctByClassPeriods_Id(
                            student.classPeriod.id);

            Map<Long, String> statuses = new HashMap<>();

            for (Assignment assignment : studentAssignments) {

                // Get all submissions for this assignment.
                // Keep only submissions belonging to this student.
                Submission latest = submissions
                        .findByAssignmentIdIn(
                                List.of(assignment.id))
                        .stream()
                        .filter(s ->
                                s.student.id.equals(student.id))
                        .max(Comparator
                                .comparing((Submission s) ->
                                        s.submittedAt)
                                .thenComparing(s -> s.id))
                        .orElse(null);

                String status;

                if (latest == null) {
                    status = "IN_PROGRESS";

                } else if (latest.grade == null) {
                    status = "SUBMITTED";

                } else if (assignment.maxPoints > 0 &&
                        latest.grade >= assignment.maxPoints) {
                    status = "GRADED_100";

                } else {
                    status = "NEEDS_WORK";
                }

                statuses.put(assignment.id, status);
            }

            model.addAttribute(
                    "assignments", studentAssignments);

            model.addAttribute(
                    "assignmentStatuses", statuses);
        }

        return "student";
    }

    @GetMapping("/student/sandbox")
    public String sandbox() {
        return "sandbox";
    }

    @GetMapping("/student/assignment/{id}")
    public String openAssignment(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        Student student = currentStudent(principal);

        Assignment assignment = assignments.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        if (student.classPeriod == null ||
                !assignment.classPeriods.stream()
                        .anyMatch(period ->
                                period.id.equals(
                                        student.classPeriod.id))) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }


        model.addAttribute("assignment", assignment);

// Find this student's most recent submission
// for the assignment.
        Submission latestSubmission = submissions
                .findByAssignmentIdIn(List.of(assignment.id))
                .stream()
                .filter(s -> s.student.id.equals(student.id))
                .max(Comparator
                        .comparing((Submission s) -> s.submittedAt)
                        .thenComparing(s -> s.id))
                .orElse(null);

// Only show teacher feedback when the most recent
// submission has been graded.
        Submission gradedSubmission =
                latestSubmission != null && latestSubmission.grade != null
                        ? latestSubmission
                        : null;

        model.addAttribute("gradedSubmission", gradedSubmission);

        return "editor";

    }

    private Student currentStudent(Principal principal) {
        return students.findByUsername(
                principal.getName()
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN));
    }
}
