package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.Assignment;
import com.laf.cobabe_code_lms.models.AssignmentDraft;
import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.AssignmentDrafts;
import com.laf.cobabe_code_lms.repositories.Assignments;
import com.laf.cobabe_code_lms.repositories.Students;

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
    private final Assignments assignments;
    private final Students students;

    public AssignmentDraftController(
            AssignmentDrafts drafts,
            Assignments assignments,
            Students students) {

        this.drafts = drafts;
        this.assignments = assignments;
        this.students = students;
    }

    public record SaveRequest(String code) {}

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
                ).map(draft -> Map.of(
                        "found", true,
                        "code", draft.code
                )).orElseGet(() -> Map.of(
                        "found", false,
                        "code", ""
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

        if (request == null || request.code() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Code is required."));
        }

        if (request.code().length() > 50000) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Code exceeds 50,000 characters."));
        }

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

        draft.code = request.code();
        draft.lastSaved = Instant.now();

        drafts.save(draft);

        return ResponseEntity.ok(
                Map.of("saved", true));
    }
}