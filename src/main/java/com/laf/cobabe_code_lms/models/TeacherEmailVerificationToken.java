
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "teacher_email_verification_tokens")
public class TeacherEmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    public Teacher teacher;

    @Column(nullable = false, unique = true, length = 64)
    public String tokenHash;

    @Column(nullable = false)
    public String emailAddress;

    @Column(nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean used = false;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
