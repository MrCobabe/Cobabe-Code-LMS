
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sandbox_programs",
        uniqueConstraints = @UniqueConstraint(columnNames = "student_id")
)
public class SandboxProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Lob
    @Column(nullable = false)
    private String code = "";

    private LocalDateTime lastSaved;

    public SandboxProgram() {
    }

    public SandboxProgram(String studentId, String code) {
        this.studentId = studentId;
        this.code = code;
        this.lastSaved = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.lastSaved = LocalDateTime.now();
    }

    public LocalDateTime getLastSaved() {
        return lastSaved;
    }
}
