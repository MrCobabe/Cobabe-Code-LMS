
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.Student;
import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.repositories.Teachers;
import org.springframework.security.core.userdetails.*;

public class DatabaseUserDetailsService implements UserDetailsService {

    private final Students students;
    private final Teachers teachers;

    public DatabaseUserDetailsService(Students students, Teachers teachers) {
        this.students = students;
        this.teachers = teachers;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Check teacher accounts first.
        Teacher teacher = teachers.findByUsername(username)
                .orElse(null);

        if (teacher != null) {

            if (teacher.passwordHash == null) {
                throw new UsernameNotFoundException(
                        "Teacher account has no password");
            }

            return User.withUsername(teacher.username)
                    .password(teacher.passwordHash)
                    .roles("TEACHER")
                    .build();
        }

        // Otherwise, look for a student account.
        Student student = students.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found"));

        if (student.passwordHash == null) {
            throw new UsernameNotFoundException(
                    "Account has no password");
        }

        return User.withUsername(student.username)
                .password(student.passwordHash)
                .roles("STUDENT")
                .build();
    }
}
