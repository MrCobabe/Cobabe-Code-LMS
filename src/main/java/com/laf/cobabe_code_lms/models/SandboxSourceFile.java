
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sandbox_source_files",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"sandbox_id", "filename"}
        )
)
public class SandboxSourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sandbox_id", nullable = false)
    private SandboxProgram sandbox;

    @Column(name = "filename", nullable = false, length = 120)
    private String filename;

    @Lob
    @Column(nullable = false)
    private String code = "";

    @Column(nullable = false)
    private LocalDateTime lastSaved = LocalDateTime.now();

    public SandboxSourceFile() {
    }

    public SandboxSourceFile(
            SandboxProgram sandbox,
            String filename,
            String code) {

        this.sandbox = sandbox;
        this.filename = filename;
        this.code = code;
        this.lastSaved = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public SandboxProgram getSandbox() {
        return sandbox;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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
