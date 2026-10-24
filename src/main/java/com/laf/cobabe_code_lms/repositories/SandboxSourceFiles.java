
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.SandboxSourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SandboxSourceFiles
        extends JpaRepository<SandboxSourceFile, Long> {

    List<SandboxSourceFile> findBySandbox_IdOrderByFilenameAsc(
            Long sandboxId);

    Optional<SandboxSourceFile> findBySandbox_IdAndFilename(
            Long sandboxId,
            String filename);

    long deleteBySandbox_Id(Long sandboxId);
}
