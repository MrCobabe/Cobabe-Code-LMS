
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(name = "showcase_projects")
public class ShowcaseProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 160)
    public String title;

    @Column(length = 12000)
    public String description;

    @Column(nullable = false, length = 9)
    public String schoolYear;

    // STUDENT or CLASS
    @Column(length = 20)
    public String projectType = "CLASS";

    // Student names or class attribution.
    @Column(length = 500)
    public String creatorCredit;

    // Generated filenames for uploaded files.
    @Column(length = 255)
    public String imageFilename;

    @Column(length = 255)
    public String jarFilename;

    @Column(nullable = false)
    public boolean published = false;

    @Column(nullable = false)
    public int displayOrder = 0;

    public ShowcaseProject() {
    }
}
