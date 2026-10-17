
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.TeacherEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherEmailVerificationTokens
        extends JpaRepository<TeacherEmailVerificationToken, Long> {

    Optional<TeacherEmailVerificationToken> findByTokenHash(String tokenHash);
}
