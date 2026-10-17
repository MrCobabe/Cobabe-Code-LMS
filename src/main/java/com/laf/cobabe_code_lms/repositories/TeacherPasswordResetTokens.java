
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.TeacherPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherPasswordResetTokens
        extends JpaRepository<TeacherPasswordResetToken, Long> {

    Optional<TeacherPasswordResetToken> findByTokenHash(String tokenHash);

}
