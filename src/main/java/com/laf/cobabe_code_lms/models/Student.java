
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
public class Student {

    @Id
    public String id;

    @Column(unique = true, nullable = true)
    public String email;

    @Column(nullable = false)
    public String name;

    public boolean teacher;

    @Column(unique = true)
    public String username;

    public String passwordHash;

    public boolean mustChangePassword = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_period_id")
    public ClassPeriod classPeriod;

    protected Student() {
    }

    // Preserve compatibility with the original constructor.
    public Student(
            String id,
            String email,
            String name,
            boolean teacher) {

        this.id = id;
        this.email = email;
        this.name = name;
        this.teacher = teacher;
    }

    public Student(
            String id,
            String name,
            String username,
            String passwordHash,
            ClassPeriod classPeriod) {

        this.id = id;
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
        this.classPeriod = classPeriod;
        this.teacher = false;
        this.mustChangePassword = true;
    }
}
