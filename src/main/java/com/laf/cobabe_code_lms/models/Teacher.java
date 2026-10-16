
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(name = "teacher_accounts")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column
    public String recoveryEmail;

    @Column(nullable = false)
    public boolean recoveryEmailVerified = false;

    @Column(nullable = false)
    public boolean mustCompleteSetup = true;
}
