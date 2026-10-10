
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.ClassPeriod;
import com.laf.cobabe_code_lms.repositories.ClassPeriods;
import com.laf.cobabe_code_lms.repositories.Students;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/classes")
public class ClassManagementController {

    private final ClassPeriods classPeriods;
    private final Students students;

    public ClassManagementController(
            ClassPeriods classPeriods,
            Students students) {

        this.classPeriods = classPeriods;
        this.students = students;
    }

    @GetMapping
    public String listClasses(Model model) {

        var classes = classPeriods.findAll();

        model.addAttribute("classes", classes);

        model.addAttribute(
                "studentCounts",
                classes.stream().collect(
                        java.util.stream.Collectors.toMap(
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
                    "error", "Invalid class name or period number.");
            return "redirect:/teacher/classes";
        }

        classPeriods.save(
                new ClassPeriod(name, periodNumber)
        );

        redirect.addFlashAttribute(
                "success", "Class created successfully!");

        return "redirect:/teacher/classes";
    }

    @PostMapping("/{id}/delete")
    public String deleteClass(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        if (!classPeriods.existsById(id)) {
            redirect.addFlashAttribute(
                    "error", "Class not found.");
            return "redirect:/teacher/classes";
        }

        if (!students.findByClassPeriodId(id).isEmpty()) {
            redirect.addFlashAttribute(
                    "error",
                    "You cannot delete a class that has students.");
            return "redirect:/teacher/classes";
        }

        classPeriods.deleteById(id);

        redirect.addFlashAttribute(
                "success", "Class deleted successfully.");

        return "redirect:/teacher/classes";
    }
}
