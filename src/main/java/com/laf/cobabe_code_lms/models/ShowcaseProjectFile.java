
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(name = "showcase_project_files")
public class ShowcaseProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // The project this file belongs to.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    public ShowcaseProject project;

    // IMAGE or JAR
    @Column(nullable = false, length = 20)
    public String fileType;

    // Original filename for downloads.
    @Column(nullable = false, length = 255)
    public String originalFilename;

    // MIME type, such as image/png or application/java-archive.
    @Column(nullable = false, length = 120)
    public String contentType;

    // DATABASE now; EXTERNAL later.
    @Column(nullable = false, length = 20)
    public String storageType = "DATABASE";

    // Reserved for future external storage.
    @Column(length = 1000)
    public String externalPath;

    // File contents stored in H2.
    // Loaded separately from project metadata.
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")
    public byte[] fileData;

    // Size in bytes.
    @Column(nullable = false)
    public long fileSize;

    public ShowcaseProjectFile() {
    }
}
