package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.AssignmentDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentDrafts
        extends JpaRepository<AssignmentDraft, Long> {

    Optional<AssignmentDraft> findByStudent_IdAndAssignment_Id(
            String studentId,
            Long assignmentId);
}