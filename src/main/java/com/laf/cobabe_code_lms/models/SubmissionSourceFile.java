
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(
        name = "submission_source_files",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"submission_id", "filename"}
        )
)
public class SubmissionSourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "filename", nullable = false, length = 120)
    private String filename;

    @Lob
    @Column(nullable = false)
    private String code = "";

    public SubmissionSourceFile() {
    }

    public SubmissionSourceFile(
            Submission submission,
            String filename,
            String code) {

        this.submission = submission;
        this.filename = filename;
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public String getFilename() {
        return filename;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
