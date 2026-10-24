
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.AssignmentDraftSourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentDraftSourceFiles
        extends JpaRepository<AssignmentDraftSourceFile, Long> {

    List<AssignmentDraftSourceFile>
    findByDraft_IdOrderByFilenameAsc(Long draftId);

    Optional<AssignmentDraftSourceFile>
    findByDraft_IdAndFilename(Long draftId, String filename);

    long deleteByDraft_Id(Long draftId);
}
