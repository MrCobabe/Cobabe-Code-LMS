
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.SubmissionSourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionSourceFiles
        extends JpaRepository<SubmissionSourceFile, Long> {

    // Retrieve all additional Java files for a submission.
    List<SubmissionSourceFile>
    findBySubmission_IdOrderByFilenameAsc(Long submissionId);

    // Delete additional files when a submission is deleted.
    long deleteBySubmission_Id(Long submissionId);
}
