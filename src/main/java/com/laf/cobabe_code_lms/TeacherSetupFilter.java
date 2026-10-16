
package com.laf.cobabe_code_lms;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.repositories.Teachers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TeacherSetupFilter extends OncePerRequestFilter {

    private final Teachers teachers;

    public TeacherSetupFilter(Teachers teachers) {
        this.teachers = teachers;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_TEACHER"))) {

            Teacher teacher = teachers
                    .findByUsername(authentication.getName())
                    .orElse(null);

            if (teacher == null) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (teacher.mustCompleteSetup) {

                String path = request.getServletPath();

                boolean allowed =
                        path.equals("/teacher/setup")
                                || path.equals("/logout")
                                || path.startsWith("/css/")
                                || path.startsWith("/js/")
                                || path.startsWith("/images/")
                                || path.equals("/style.css");

                if (!allowed) {
                    response.sendRedirect(
                            request.getContextPath()
                                    + "/teacher/setup");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
