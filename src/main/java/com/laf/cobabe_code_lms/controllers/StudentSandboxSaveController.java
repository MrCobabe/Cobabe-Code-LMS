
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RestController
@RequestMapping("/api/student/sandbox")
public class StudentSandboxSaveController {

    private final SandboxPrograms sandboxPrograms;

    public StudentSandboxSaveController(
            SandboxPrograms sandboxPrograms) {
        this.sandboxPrograms = sandboxPrograms;
    }

    @GetMapping("/load")
    public ResponseEntity<?> load(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String studentId = authentication.getName();

        return ResponseEntity.ok(
                sandboxPrograms.findByStudentId(studentId)
                        .map(program -> Map.of(
                                "found", true,
                                "code", program.getCode()
                        ))
                        .orElseGet(() -> Map.of(
                                "found", false,
                                "code", ""
                        ))
        );
    }

    public record SaveRequest(String code) {}

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<?> save(
            @RequestBody SaveRequest request,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        if (request == null || request.code() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Code is required."));
        }

        if (request.code().length() > 65536) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Code is too large."));
        }

        String studentId = authentication.getName();

        SandboxProgram program = sandboxPrograms
                .findByStudentId(studentId)
                .orElseGet(() ->
                        new SandboxProgram(studentId, "")
                );

        program.setCode(request.code());
        sandboxPrograms.save(program);

        return ResponseEntity.ok(Map.of(
                "saved", true,
                "message", "Sandbox saved successfully."
        ));
    }
}
