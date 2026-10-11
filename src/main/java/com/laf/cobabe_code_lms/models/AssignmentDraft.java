package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "assignment_drafts",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "assignment_id"}
        )
)
public class AssignmentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    public Assignment assignment;

    @Column(nullable = false, length = 50000)
    public String code = "";

    public Instant lastSaved = Instant.now();

    public AssignmentDraft() {
    }

    public AssignmentDraft(
            Student student,
            Assignment assignment,
            String code) {

        this.student = student;
        this.assignment = assignment;
        this.code = code;
        this.lastSaved = Instant.now();
    }
}