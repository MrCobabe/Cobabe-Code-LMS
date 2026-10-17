
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.models.TeacherPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface TeacherPasswordResetTokens
        extends JpaRepository<TeacherPasswordResetToken, Long> {

    Optional<TeacherPasswordResetToken> findByTokenHash(
            String tokenHash);

    boolean existsByTeacherAndCreatedAtAfter(
            Teacher teacher,
            Instant createdAt);

    long countByTeacherAndCreatedAtAfter(
            Teacher teacher,
            Instant createdAt);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        UPDATE TeacherPasswordResetToken t
        SET t.used = true
        WHERE t.teacher = :teacher
        AND t.used = false
        """)
    int invalidateAllForTeacher(
            @org.springframework.data.repository.query.Param("teacher")
            Teacher teacher);

}
