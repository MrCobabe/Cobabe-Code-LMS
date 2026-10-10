
package com.laf.cobabe_code_lms;

import com.laf.cobabe_code_lms.repositories.Students;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class StudentPasswordChangeFilter
        extends OncePerRequestFilter {

    private final Students students;

    public StudentPasswordChangeFilter(Students students) {
        this.students = students;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {

            boolean isStudent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority()
                            .equals("ROLE_STUDENT"));

            if (isStudent) {
                var student = students
                        .findByUsername(auth.getName())
                        .orElse(null);

                if (student != null && student.mustChangePassword) {

                    String path = request.getServletPath();

                    boolean allowed =
                            path.equals("/student/password")
                                    || path.equals("/logout")
                                    || path.equals("/")
                                    || path.startsWith("/css/")
                                    || path.startsWith("/js/")
                                    || path.startsWith("/images/")
                                    || path.equals("/style.css");

                    if (!allowed) {
                        response.sendRedirect(
                                request.getContextPath()
                                        + "/student/password");
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }
}
