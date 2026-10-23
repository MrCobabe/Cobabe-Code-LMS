
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.ClassPeriod;
import com.laf.cobabe_code_lms.models.Student;

import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import com.laf.cobabe_code_lms.repositories.ClassPeriods;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Submissions;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher/classes")
public class ClassManagementController {

    private final ClassPeriods classPeriods;
    private final Students students;
    private final Assignments assignments;
    private final Submissions submissions;
    private final AssignmentDrafts drafts;
    private final SandboxPrograms sandboxes;

    public ClassManagementController(
            ClassPeriods classPeriods,
            Students students,
            Assignments assignments,
            Submissions submissions,
            AssignmentDrafts drafts,
            SandboxPrograms sandboxes) {

        this.classPeriods = classPeriods;
        this.students = students;
        this.assignments = assignments;
        this.submissions = submissions;
        this.drafts = drafts;
        this.sandboxes = sandboxes;
    }

    @GetMapping
    public String listClasses(Model model) {

        var classes = classPeriods.findAll();

        model.addAttribute("classes", classes);

        model.addAttribute(
                "studentCounts",
                classes.stream().collect(
                        Collectors.toMap(
                                c -> c.id,
                                c -> students.findByClassPeriodId(c.id).size()
                        )
                )
        );

        return "classes";
    }

    @PostMapping("/create")
    public String createClass(
            @RequestParam String name,
            @RequestParam int periodNumber,
            RedirectAttributes redirect) {

        name = name.trim();

        if (name.isBlank() || name.length() > 100 ||
                periodNumber < 1 || periodNumber > 20) {

            redirect.addFlashAttribute(
                    "error",
                    "Invalid class name or period number."
            );

            return "redirect:/teacher/classes";
        }

        classPeriods.save(
                new ClassPeriod(name, periodNumber)
        );

        redirect.addFlashAttribute(
                "success",
                "Class created successfully!"
        );

        return "redirect:/teacher/classes";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteClass(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        ClassPeriod classPeriod =
                classPeriods.findById(id).orElse(null);

        if (classPeriod == null) {

            redirect.addFlashAttribute(
                    "error",
                    "Class not found."
            );

            return "redirect:/teacher/classes";
        }

        List<Student> classStudents =
                students.findByClassPeriodId(id);

        // Never delete a teacher account.
        boolean containsTeacher = classStudents.stream()
                .anyMatch(student -> student.teacher);

        if (containsTeacher) {
            redirect.addFlashAttribute(
                    "error",
                    "Class deletion blocked because it contains " +
                            "a teacher account."
            );

            return "redirect:/teacher/classes";
        }

        List<Assignment> classAssignments =
                assignments.findDistinctByClassPeriods_Id(id);

        // Identify assignments belonging exclusively
        // to this class.
        Set<Long> exclusiveAssignmentIds = new HashSet<>();

        for (Assignment assignment : classAssignments) {

            if (assignment.classPeriods.size() == 1) {
                exclusiveAssignmentIds.add(assignment.id);
            }
        }

        int deletedStudents = 0;
        int deletedAssignments = 0;
        int preservedAssignments = 0;

        // STEP 1: Delete each student's dependent data.

        for (Student student : classStudents) {

            submissions.deleteByStudentId(student.id);

            drafts.deleteByStudent_Id(student.id);

            sandboxes.deleteByStudentId(student.username);

            // Flush dependent deletions before
            // removing the student account.
            submissions.flush();
            drafts.flush();
            sandboxes.flush();

            students.delete(student);

            deletedStudents++;
        }

        students.flush();

        // STEP 2: Remove any remaining submissions
        // and drafts referencing assignments that
        // belong exclusively to this class.
        //
        // This handles unexpected records belonging
        // to students outside the deleted class.

        if (!exclusiveAssignmentIds.isEmpty()) {

            var remainingSubmissions =
                    submissions.findByAssignmentIdIn(
                            exclusiveAssignmentIds
                    );

            submissions.deleteAll(remainingSubmissions);
            submissions.flush();

            var remainingDrafts = drafts.findAll().stream()
                    .filter(draft ->
                            draft.assignment != null &&
                                    exclusiveAssignmentIds.contains(
                                            draft.assignment.id
                                    )
                    )
                    .toList();

            drafts.deleteAll(remainingDrafts);
            drafts.flush();
        }

        // STEP 3: Remove class associations and
        // delete exclusive assignments.

        for (Assignment assignment : classAssignments) {

            assignment.classPeriods.removeIf(
                    associatedClass ->
                            associatedClass.id.equals(id)
            );

            if (exclusiveAssignmentIds.contains(assignment.id)) {

                assignments.delete(assignment);
                deletedAssignments++;

            } else {

                assignments.save(assignment);
                preservedAssignments++;
            }
        }

        assignments.flush();

        // STEP 4: Delete the class itself.

        classPeriods.delete(classPeriod);
        classPeriods.flush();

        redirect.addFlashAttribute(
                "success",
                "Class deleted successfully. " +
                        deletedStudents + " students and " +
                        deletedAssignments + " exclusive assignments removed. " +
                        preservedAssignments + " shared assignments preserved."
        );

        return "redirect:/teacher/classes";
    }
}
