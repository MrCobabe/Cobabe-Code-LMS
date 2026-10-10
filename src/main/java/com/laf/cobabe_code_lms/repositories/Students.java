
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Students extends JpaRepository<Student, String> {

    Optional<Student> findByUsername(String username);

    List<Student> findByClassPeriodId(Long classPeriodId);
}
