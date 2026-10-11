
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SandboxPrograms
        extends JpaRepository<SandboxProgram, Long> {

    Optional<SandboxProgram> findByStudentId(String studentId);
}
