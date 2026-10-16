
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.InstallationState;
import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.repositories.InstallationStates;
import com.laf.cobabe_code_lms.repositories.Teachers;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class TeacherInitializer {

    @Bean
    public ApplicationRunner initializeTeacher(
            Teachers teachers,
            InstallationStates installationStates,
            PasswordEncoder encoder) {

        return args -> {

            // Check whether this installation has been initialized.
            InstallationState state = installationStates
                    .findById(1L)
                    .orElse(null);

            if (state != null && state.teacherInitialized) {
                return;
            }

            // An earlier version may already have created
            // the teacher account before installation tracking existed.
            if (teachers.count() > 0) {

                InstallationState existingState =
                        state != null ? state : new InstallationState();

                existingState.teacherInitialized = true;
                installationStates.save(existingState);

                System.out.println(
                        "Existing teacher account recognized."
                );

                return;
            }

            // First-time initialization only.
            Teacher teacher = new Teacher();
            teacher.username = "teacher";
            teacher.passwordHash = encoder.encode("123456");
            teacher.mustCompleteSetup = true;
            teacher.recoveryEmailVerified = false;

            teachers.save(teacher);

            InstallationState newState =
                    state != null ? state : new InstallationState();

            newState.teacherInitialized = true;
            installationStates.save(newState);

            System.out.println(
                    "Initial teacher account created. " +
                            "Complete setup before exposing the LMS publicly."
            );
        };
    }
}
