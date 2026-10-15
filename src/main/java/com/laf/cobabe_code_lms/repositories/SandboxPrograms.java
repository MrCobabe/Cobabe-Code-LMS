
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.SandboxProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SandboxPrograms
        extends JpaRepository<SandboxProgram, Long> {

    List<SandboxProgram> findByStudentIdOrderByLastSavedDesc(
            String studentId
    );

    Optional<SandboxProgram> findByIdAndStudentId(
            Long id,
            String studentId
    );

    Optional<SandboxProgram> findByStudentId(
            String studentId
    );

    long deleteByStudentId(String studentId);
}
