package com.laf.cobabe_code_lms.repositories;



import com.laf.cobabe_code_lms.models.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Assignments
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findDistinctByClassPeriods_Id(
            Long classPeriodId);
}
