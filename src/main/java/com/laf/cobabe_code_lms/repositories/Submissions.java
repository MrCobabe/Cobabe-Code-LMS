
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Submissions extends JpaRepository<Submission, Long> {

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(
            String studentId);

    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(
            Long assignmentId);

    long deleteByStudentId(String studentId);
}
