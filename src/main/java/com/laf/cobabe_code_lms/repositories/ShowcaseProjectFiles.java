
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.ShowcaseProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowcaseProjectFiles
        extends JpaRepository<ShowcaseProjectFile, Long> {

    List<ShowcaseProjectFile> findByProjectId(Long projectId);

    Optional<ShowcaseProjectFile>
    findByProjectIdAndFileType(
            Long projectId,
            String fileType
    );

    void deleteByProjectId(Long projectId);
}
