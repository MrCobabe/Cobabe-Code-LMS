
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sandbox_programs")
public class SandboxProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "name", length = 120)
    private String name = "My Sandbox";

    @Lob
    @Column(nullable = false)
    private String code = "";

    private LocalDateTime lastSaved;

    public SandboxProgram() {
    }

    public SandboxProgram(String studentId, String code) {
        this.studentId = studentId;
        this.code = code;
        this.name = "My Sandbox";
        this.lastSaved = LocalDateTime.now();
    }

    public SandboxProgram(
            String studentId,
            String name,
            String code) {

        this.studentId = studentId;
        this.name = name;
        this.code = code;
        this.lastSaved = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name == null ? "My Sandbox" : name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastSaved = LocalDateTime.now();
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
