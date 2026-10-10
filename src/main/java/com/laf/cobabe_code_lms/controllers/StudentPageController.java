
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

@Controller
public class StudentPageController {

    private final Students students;
    private final Assignments assignments;

    public StudentPageController(
            Students students,
            Assignments assignments) {

        this.students = students;
        this.assignments = assignments;
    }

    @GetMapping("/student")
    public String dashboard(
            Principal principal,
            Model model) {

        Student student = currentStudent(principal);

        model.addAttribute(
                "studentName", student.name);

        if (student.classPeriod == null) {
            model.addAttribute(
                    "className", "No class assigned");
            model.addAttribute(
                    "assignments", List.of());
        } else {
            model.addAttribute(
                    "className",
                    student.classPeriod.name +
                            " — Period " +
                            student.classPeriod.periodNumber);

            model.addAttribute(
                    "assignments",
                    assignments.findDistinctByClassPeriods_Id(
                            student.classPeriod.id));
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
