
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class TeacherController {

    private final SandboxPrograms sandboxPrograms;

    public TeacherController(
            SandboxPrograms sandboxPrograms) {
        this.sandboxPrograms = sandboxPrograms;
    }

    @GetMapping("/teacher")
    public String teacherDashboard() {
        return "teacher";
    }

    @GetMapping("/runner-test")
    public String runnerTestPage() {
        return "runner-test";
    }

    @GetMapping("/teacher/sandboxes")
    public String teacherSandboxes(Model model) {
        model.addAttribute("teacherMode", true);
        return "sandboxes";
    }

    @GetMapping("/teacher/sandbox/{id}")
    public String teacherSandbox(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        SandboxProgram program = sandboxPrograms
                .findByIdAndStudentId(
                        id,
                        principal.getName()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Sandbox not found"
                        )
                );

        model.addAttribute("teacherMode", true);
        model.addAttribute("sandboxId", program.getId());

        return "sandbox";
    }
}
