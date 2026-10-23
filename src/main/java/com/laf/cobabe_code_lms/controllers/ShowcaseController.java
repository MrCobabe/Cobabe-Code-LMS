
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.ShowcaseProject;
import com.laf.cobabe_code_lms.repositories.ShowcaseProjects;

import com.laf.cobabe_code_lms.services.ShowcaseFileService;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ShowcaseController {

    private final ShowcaseProjects showcaseProjects;
    private final JdbcTemplate jdbc;
    private final ShowcaseFileService showcaseFileService;

    public ShowcaseController(
            ShowcaseProjects showcaseProjects,
            JdbcTemplate jdbc, ShowcaseFileService showcaseFileService) {

        this.showcaseProjects = showcaseProjects;
        this.jdbc = jdbc;
        this.showcaseFileService = showcaseFileService;
    }


    @GetMapping("/showcase")
    public String showcase(Model model) {

        List<ShowcaseProject> projects = showcaseProjects
                .findByPublishedTrueOrderBySchoolYearDescDisplayOrderAsc();

        Map<String, List<ShowcaseProject>> projectsByYear =
                projects.stream()
                        .collect(Collectors.groupingBy(
                                project -> project.schoolYear,
                                java.util.LinkedHashMap::new,
                                Collectors.toList()
                        ));

        Map<Long, Boolean> hasImages = new java.util.HashMap<>();
        Map<Long, Boolean> hasJars = new java.util.HashMap<>();

        for (ShowcaseProject project : projects) {

            hasImages.put(
                    project.id,
                    showcaseFileService.hasFile(project.id, "IMAGE")
            );

            hasJars.put(
                    project.id,
                    showcaseFileService.hasFile(project.id, "JAR")
            );
        }

        model.addAttribute("projectsByYear", projectsByYear);
        model.addAttribute("hasImages", hasImages);
        model.addAttribute("hasJars", hasJars);

        return "showcase";
    }


    @GetMapping("/showcase/{id}/image")
    public void image(
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {

        serveFile(id, "IMAGE", response);
    }

    @GetMapping("/showcase/{id}/download")
    public void download(
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {

        serveFile(id, "JAR", response);
    }


    private void serveFile(
            Long projectId,
            String fileType,
            HttpServletResponse response) throws IOException {

        ShowcaseProject project = showcaseProjects
                .findById(projectId)
                .orElse(null);

        // Never expose unpublished projects.
        if (project == null || !project.published) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            Boolean found = jdbc.execute(
                    (org.springframework.jdbc.core.ConnectionCallback<Boolean>)
                            connection -> {

                                String sql = """
                    SELECT original_filename,
                           content_type,
                           file_size,
                           file_data
                    FROM showcase_project_files
                    WHERE project_id = ?
                      AND file_type = ?
                    """;

                                try (PreparedStatement statement =
                                             connection.prepareStatement(sql)) {

                                    statement.setLong(1, projectId);
                                    statement.setString(2, fileType);

                                    try (ResultSet results = statement.executeQuery()) {

                                        if (!results.next()) {
                                            return false;
                                        }

                                        String contentType =
                                                results.getString("content_type");

                                        long fileSize =
                                                results.getLong("file_size");

                                        response.setHeader(
                                                "X-Content-Type-Options", "nosniff");

                                        response.setHeader(
                                                "Cache-Control", "no-store");

                                        if ("IMAGE".equals(fileType)) {

                                            if (!List.of(
                                                    "image/png",
                                                    "image/jpeg",
                                                    "image/webp"
                                            ).contains(contentType)) {
                                                throw new java.sql.SQLException(
                                                        "Unsupported image type");
                                            }

                                            response.setContentType(contentType);
                                            response.setHeader(
                                                    "Content-Disposition", "inline");

                                        } else {

                                            response.setContentType(
                                                    "application/java-archive");

                                            response.setHeader(
                                                    "Content-Disposition",
                                                    "attachment; filename=\"project-" +
                                                            projectId + ".jar\"");
                                        }

                                        response.setContentLengthLong(fileSize);

                                        try (InputStream input =
                                                     results.getBinaryStream("file_data")) {

                                            OutputStream output =
                                                    response.getOutputStream();

                                            input.transferTo(output);
                                            output.flush();
                                        } catch (IOException exception) {
                                            throw new java.sql.SQLException(
                                                    "File transfer failed", exception);
                                        }

                                        return true;
                                    }
                                }
                            });

            if (!Boolean.TRUE.equals(found)
                    && !response.isCommitted()) {

                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception exception) {

            if (!response.isCommitted()) {
                response.reset();
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                throw new IOException(
                        "File transfer failed", exception);
            }
        }
    }

}
