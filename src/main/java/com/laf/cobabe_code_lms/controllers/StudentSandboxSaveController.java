
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import com.laf.cobabe_code_lms.repositories.SandboxPrograms;
import com.laf.cobabe_code_lms.models.SandboxSourceFile;
import com.laf.cobabe_code_lms.repositories.SandboxSourceFiles;
import java.util.LinkedHashMap;
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
    private final SandboxSourceFiles sandboxSourceFiles;

    public StudentSandboxSaveController(
            SandboxPrograms sandboxPrograms, SandboxSourceFiles sandboxSourceFiles) {
        this.sandboxPrograms = sandboxPrograms;
        this.sandboxSourceFiles = sandboxSourceFiles;
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
                .<ResponseEntity<?>>map(program -> {

                    Map<String, String> files = new LinkedHashMap<>();

                    // Preserve existing Sandbox code as Main.java
                    files.put("Main.java", program.getCode());

                    // Load additional Java files
                    for (SandboxSourceFile sourceFile :
                            sandboxSourceFiles.findBySandbox_IdOrderByFilenameAsc(
                                    program.getId())) {

                        files.put(
                                sourceFile.getFilename(),
                                sourceFile.getCode()
                        );
                    }

                    return ResponseEntity.ok(Map.of(
                            "found", true,
                            "id", program.getId(),
                            "name", program.getName(),
                            "code", program.getCode(),
                            "files", files
                    ));
                })
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
    public record SaveRequest(
            String code,
            Map<String, String> files
    ) {}


    @PostMapping("/{id}/save")
    @Transactional
    public ResponseEntity<?> save(
            @PathVariable Long id,
            @RequestBody SaveRequest request,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Code is required."));
        }

        Map<String, String> files = request.files();

        // Old single-file editor compatibility.
        boolean multiFileSave = files != null;

        if (!multiFileSave && request.code() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Code is required."));
        }

        if (multiFileSave) {

            // Main.java must always exist.
            if (!files.containsKey("Main.java")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Main.java is required."));
            }

            // Limit each Sandbox to 20 Java files.
            if (files.size() > 20) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Too many Java files."));
            }

            long totalSize = 0;

            for (Map.Entry<String, String> entry : files.entrySet()) {

                String filename = entry.getKey();
                String code = entry.getValue();

                // Only simple Java filenames; no directories.
                if (filename == null
                        || !filename.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")
                        || filename.length() > 120) {

                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid Java filename."));
                }

                if (code == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "File code cannot be null."));
                }

                totalSize += code.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8).length;

                if (totalSize > 65536) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Total code is too large."));
                }
            }

        } else if (request.code().length() > 65536) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Code is too large."));
        }

        return sandboxPrograms
                .findByIdAndStudentId(id, authentication.getName())
                .<ResponseEntity<?>>map(program -> {

                    if (!multiFileSave) {

                        // Legacy saves only update Main.java.
                        // Additional files remain untouched.
                        program.setCode(request.code());

                    } else {

                        program.setCode(files.get("Main.java"));

                        // Remove files omitted from the new file list.
                        List<SandboxSourceFile> existingFiles =
                                sandboxSourceFiles
                                        .findBySandbox_IdOrderByFilenameAsc(
                                                program.getId());

                        for (SandboxSourceFile existing : existingFiles) {
                            if (!files.containsKey(existing.getFilename())
                                    || "Main.java".equals(existing.getFilename())) {
                                sandboxSourceFiles.delete(existing);
                            }
                        }

                        // Insert or update additional Java files.
                        for (Map.Entry<String, String> entry : files.entrySet()) {

                            String filename = entry.getKey();

                            if ("Main.java".equals(filename)) {
                                continue;
                            }

                            SandboxSourceFile sourceFile =
                                    existingFiles.stream()
                                            .filter(f -> f.getFilename()
                                                    .equals(filename))
                                            .findFirst()
                                            .orElse(null);

                            if (sourceFile == null) {
                                sourceFile = new SandboxSourceFile(
                                        program,
                                        filename,
                                        entry.getValue());
                            } else {
                                sourceFile.setCode(entry.getValue());
                            }

                            sandboxSourceFiles.save(sourceFile);
                        }
                    }

                    sandboxPrograms.save(program);

                    return ResponseEntity.ok(Map.of(
                            "saved", true,
                            "id", program.getId(),
                            "message", "Sandbox saved successfully."
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
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

                    // Delete additional Java files first.
                    sandboxSourceFiles.deleteBySandbox_Id(program.getId());
                    sandboxSourceFiles.flush();

                    // Then delete the Sandbox itself.
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
