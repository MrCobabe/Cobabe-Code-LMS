
package com.laf.cobabe_code_lms;

import com.laf.cobabe_code_lms.repositories.Students;
import com.laf.cobabe_code_lms.services.DatabaseUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            Students students,
            PasswordEncoder encoder) {

        DatabaseUserDetailsService database =
                new DatabaseUserDetailsService(students);

        InMemoryUserDetailsManager temporaryTeacher =
                new InMemoryUserDetailsManager(
                        User.withUsername("teacher")
                                .password(encoder.encode("ChangeMe123!"))
                                .roles("TEACHER")
                                .build()
                );

        return username -> {
            if ("teacher".equals(username)) {
                return temporaryTeacher
                        .loadUserByUsername(username);
            }
            return database.loadUserByUsername(username);
        };
    }

    @Bean
    public SecurityFilterChain chain(
            HttpSecurity http,
            Students students,
     StudentPasswordChangeFilter passwordFilter) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/style.css",
                        "/css/**", "/js/**", "/images/**", "/h2-console/**"
                ).permitAll()

                .requestMatchers(
                        "/teacher", "/teacher/**"
                ).hasRole("TEACHER")

                .requestMatchers(
                        "/student", "/student/**",
                        "/api/student/**"
                ).hasRole("STUDENT")

                .anyRequest().authenticated()

        );
        // NEW: Allow H2 Console form submissions delete later
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
        );

        // NEW: Allow H2 Console to display its frames delete later
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
        );

        http.formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/login")
                .failureUrl("/?error")

                .successHandler((request, response, auth) -> {

                    boolean teacher = auth.getAuthorities()
                            .stream()
                            .anyMatch(a ->
                                    a.getAuthority().equals("ROLE_TEACHER"));

                    if (teacher) {
                        response.sendRedirect(
                                request.getContextPath() + "/teacher");
                        return;
                    }

                    var student = students.findByUsername(auth.getName())
                            .orElseThrow();

                    String destination = student.mustChangePassword
                            ? "/student/password"
                            : "/student";

                    response.sendRedirect(
                            request.getContextPath() + destination);
                })

                .permitAll()
        );

        http.logout(logout -> logout
                .logoutSuccessUrl("/?logout")
                .permitAll()
        );
        http.addFilterAfter(
                passwordFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean
            <StudentPasswordChangeFilter> passwordFilterRegistration(
            StudentPasswordChangeFilter filter) {

        var registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }
}
