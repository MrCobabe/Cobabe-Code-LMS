package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Teachers extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUsername(String username);

}