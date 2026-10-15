
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({
        "/api/student/sandbox",
        "/teacher/api/sandbox"
})
public class StudentSandboxSaveController {

    private final SandboxPrograms sandboxPrograms;

    public StudentSandboxSaveController(
            SandboxPrograms sandboxPrograms) {
        this.sandboxPrograms = sandboxPrograms;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        List<Map<String, Object>> programs = sandboxPrograms
                .findByStudentIdOrderByLastSavedDesc(
                        authentication.getName())
                .stream()
                .map(program -> Map.<String, Object>of(
                        "id", program.getId(),
                        "name", program.getName(),
                        "lastSaved",
                        program.getLastSaved() == null
                                ? ""
                                : program.getLastSaved().toString()
                ))
                .toList();

        return ResponseEntity.ok(programs);
    }

    @GetMapping("/{id}/load")
    public ResponseEntity<?> load(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        return sandboxPrograms
                .findByIdAndStudentId(
                        id, authentication.getName())
                .<ResponseEntity<?>>map(program ->
                        ResponseEntity.ok(Map.of(
                                "found", true,
                                "id", program.getId(),
                                "name", program.getName(),
                                "code", program.getCode()
                        ))
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    public record CreateRequest(String name) {}

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<?> create(
            @RequestBody CreateRequest request,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        String name = normalizeName(
                request == null ? null : request.name()
        );

        String starterCode = """
                public class Main {
                    public static void main(String[] args) {

                    }
                }
                """;

        SandboxProgram program = new SandboxProgram(
                authentication.getName(),
                name,
                starterCode
        );

        sandboxPrograms.save(program);

        return ResponseEntity.ok(Map.of(
                "created", true,
                "id", program.getId(),
                "name", program.getName()
        ));
    }

    public record SaveRequest(String code) {}

    @PostMapping("/{id}/save")
    @Transactional
    public ResponseEntity<?> save(
            @PathVariable Long id,
            @RequestBody SaveRequest request,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        if (request == null || request.code() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Code is required."
                    ));
        }

        if (request.code().length() > 65536) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Code is too large."
                    ));
        }

        return sandboxPrograms
                .findByIdAndStudentId(
                        id, authentication.getName())
                .<ResponseEntity<?>>map(program -> {

                    program.setCode(request.code());
                    sandboxPrograms.save(program);

                    return ResponseEntity.ok(Map.of(
                            "saved", true,
                            "id", program.getId(),
                            "message",
                            "Sandbox saved successfully."
                    ));
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    public record RenameRequest(String name) {}

    @PostMapping("/{id}/rename")
    @Transactional
    public ResponseEntity<?> rename(
            @PathVariable Long id,
            @RequestBody RenameRequest request,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        String name = normalizeName(
                request == null ? null : request.name()
        );

        return sandboxPrograms
                .findByIdAndStudentId(
                        id, authentication.getName())
                .<ResponseEntity<?>>map(program -> {

                    program.setName(name);
                    sandboxPrograms.save(program);

                    return ResponseEntity.ok(Map.of(
                            "renamed", true,
                            "id", program.getId(),
                            "name", program.getName()
                    ));
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        return sandboxPrograms
                .findByIdAndStudentId(
                        id, authentication.getName())
                .<ResponseEntity<?>>map(program -> {

                    sandboxPrograms.delete(program);

                    return ResponseEntity.ok(Map.of(
                            "deleted", true
                    ));
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    private boolean isAuthenticated(
            Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated();
    }

    private String normalizeName(String name) {

        if (name == null || name.isBlank()) {
            return "Untitled Sandbox";
        }

        String trimmed = name.trim();

        return trimmed.length() > 120
                ? trimmed.substring(0, 120)
                : trimmed;
    }
}
