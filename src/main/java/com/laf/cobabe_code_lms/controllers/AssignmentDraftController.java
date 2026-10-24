package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.AssignmentDraft;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.Students;

import com.laf.cobabe_code_lms.models.AssignmentDraftSourceFile;
import com.laf.cobabe_code_lms.repositories.AssignmentDraftSourceFiles;

import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/student/assignments")
public class AssignmentDraftController {

    private final AssignmentDrafts drafts;
    private final AssignmentDraftSourceFiles draftSourceFiles;
    private final Assignments assignments;
    private final Students students;


    public AssignmentDraftController(
            AssignmentDrafts drafts, AssignmentDraftSourceFiles draftSourceFiles,
            Assignments assignments,
            Students students) {

        this.drafts = drafts;
        this.draftSourceFiles = draftSourceFiles;
        this.assignments = assignments;
        this.students = students;
    }


    public record SaveRequest(
            String code,
            Map<String, String> files
    ) {}


    private record Access(
            Student student,
            Assignment assignment) {}

    private Access checkAccess(
            Long assignmentId,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED);
        }

        Student student = students
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN));

        if (student.teacher || student.classPeriod == null ||
                student.classPeriod.id == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN);
        }

        Assignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        boolean allowed = assignment.classPeriods.stream()
                .anyMatch(period ->
                        Objects.equals(
                                period.id,
                                student.classPeriod.id));

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }

        return new Access(student, assignment);
    }


    @GetMapping("/{assignmentId}/draft")
    @Transactional(readOnly = true)
    public ResponseEntity<?> load(
            @PathVariable Long assignmentId,
            Authentication authentication) {

        Access access = checkAccess(
                assignmentId, authentication);

        return ResponseEntity.ok(
                drafts.findByStudent_IdAndAssignment_Id(
                        access.student().id,
                        assignmentId
                ).map(draft -> {

                    Map<String, String> files = new LinkedHashMap<>();

                    // Main.java remains in the original draft record.
                    files.put("Main.java", draft.code);

                    // Load additional Java files.
                    for (AssignmentDraftSourceFile sourceFile :
                            draftSourceFiles.findByDraft_IdOrderByFilenameAsc(
                                    draft.id)) {

                        files.put(
                                sourceFile.getFilename(),
                                sourceFile.getCode()
                        );
                    }

                    return Map.of(
                            "found", true,
                            "code", draft.code,
                            "files", files
                    );

                }).orElseGet(() -> Map.of(
                        "found", false,
                        "code", "",
                        "files", Map.of()
                ))
        );
    }



    @PostMapping("/{assignmentId}/draft")
    @Transactional
    public ResponseEntity<?> save(
            @PathVariable Long assignmentId,
            @RequestBody SaveRequest request,
            Authentication authentication) {

        Access access = checkAccess(
                assignmentId, authentication);

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Save request is required."));
        }

        // Support both the original single-file format
        // and the new multi-file format.
        boolean multiFileSave = request.files() != null;

        Map<String, String> files = new LinkedHashMap<>();

        if (multiFileSave) {
            files.putAll(request.files());
        } else {
            if (request.code() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Code is required."));
            }

            files.put("Main.java", request.code());
        }

        // Main.java must always exist.
        if (!files.containsKey("Main.java")
                || files.get("Main.java") == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Main.java is required."));
        }

        // Limit the number of Java source files.
        if (files.size() > 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Maximum of 20 Java files allowed."));
        }

        // Validate filenames and total source size.
        long totalBytes = 0;

        for (Map.Entry<String, String> entry : files.entrySet()) {

            String filename = entry.getKey();
            String code = entry.getValue();

            if (filename == null
                    || filename.length() > 120
                    || !filename.matches(
                    "[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid Java filename."));
            }

            if (code == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "File code cannot be null."));
            }

            totalBytes += code.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
            ).length;

            if (totalBytes > 65536) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Total Java source exceeds 64 KB."));
            }
        }

        // Find or create the student's draft.
        AssignmentDraft draft = drafts
                .findByStudent_IdAndAssignment_Id(
                        access.student().id,
                        assignmentId
                )
                .orElseGet(() ->
                        new AssignmentDraft(
                                access.student(),
                                access.assignment(),
                                ""
                        ));

        // Keep Main.java in the original draft field.
        draft.code = files.get("Main.java");
        draft.lastSaved = Instant.now();

        drafts.saveAndFlush(draft);

        // Legacy saves update only Main.java.
        // They must not erase additional Java files.
        if (multiFileSave) {

            // Remove files no longer included in the request.
            for (AssignmentDraftSourceFile existing :
                    draftSourceFiles.findByDraft_IdOrderByFilenameAsc(
                            draft.id)) {

                if (!files.containsKey(existing.getFilename())) {
                    draftSourceFiles.delete(existing);
                }
            }

            draftSourceFiles.flush();

            // Create or update all additional files.
            for (Map.Entry<String, String> entry : files.entrySet()) {

                String filename = entry.getKey();

                if ("Main.java".equals(filename)) {
                    continue;
                }

                AssignmentDraftSourceFile sourceFile =
                        draftSourceFiles
                                .findByDraft_IdAndFilename(
                                        draft.id,
                                        filename
                                )
                                .orElseGet(() ->
                                        new AssignmentDraftSourceFile(
                                                draft,
                                                filename,
                                                ""
                                        ));

                sourceFile.setCode(entry.getValue());

                draftSourceFiles.save(sourceFile);
            }
        }

        return ResponseEntity.ok(
                Map.of("saved", true));
    }

}