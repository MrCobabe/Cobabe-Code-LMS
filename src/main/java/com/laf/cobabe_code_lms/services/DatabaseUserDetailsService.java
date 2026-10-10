
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.repositories.Students;
import org.springframework.security.core.userdetails.*;


public class DatabaseUserDetailsService
        implements UserDetailsService {

    private final Students students;

    public DatabaseUserDetailsService(Students students) {
        this.students = students;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Student student = students.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Account not found"));

        if (student.passwordHash == null) {
            throw new UsernameNotFoundException(
                    "Account has no password");
        }

        return User.withUsername(student.username)
                .password(student.passwordHash)
                .roles(student.teacher ? "TEACHER" : "STUDENT")
                .build();
    }
}
