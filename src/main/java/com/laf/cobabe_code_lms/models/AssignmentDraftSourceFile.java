
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "assignment_draft_source_files",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"draft_id", "filename"}
        )
)
public class AssignmentDraftSourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private AssignmentDraft draft;

    @Column(name = "filename", nullable = false, length = 120)
    private String filename;

    @Lob
    @Column(nullable = false)
    private String code = "";

    @Column(nullable = false)
    private Instant lastSaved = Instant.now();

    public AssignmentDraftSourceFile() {
    }

    public AssignmentDraftSourceFile(
            AssignmentDraft draft,
            String filename,
            String code) {

        this.draft = draft;
        this.filename = filename;
        this.code = code;
        this.lastSaved = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AssignmentDraft getDraft() {
        return draft;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        this.lastSaved = Instant.now();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.lastSaved = Instant.now();
    }

    public Instant getLastSaved() {
        return lastSaved;
    }
}
