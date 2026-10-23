
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.ShowcaseProject;
import com.laf.cobabe_code_lms.repositories.ShowcaseProjects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.NoSuchElementException;

@Service
public class ShowcaseProjectService {

    private final ShowcaseProjects projects;
    private final ShowcaseFileService fileService;

    public ShowcaseProjectService(
            ShowcaseProjects projects,
            ShowcaseFileService fileService) {
        this.projects = projects;
        this.fileService = fileService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(
            String title,
            String description,
            String schoolYear,
            int displayOrder,
            String projectType,
            String creatorCredit,
            MultipartFile screenshot,
            MultipartFile jarFile
    ) throws IOException {

        ShowcaseProject project = new ShowcaseProject();

        setFields(project, title, description, schoolYear,
                displayOrder, projectType, creatorCredit);

        project.published = false;
        projects.saveAndFlush(project);

        fileService.saveFile(project, "IMAGE", screenshot);
        fileService.saveFile(project, "JAR", jarFile);
    }

    @Transactional(rollbackFor = Exception.class)
    public void edit(
            Long id,
            String title,
            String description,
            String schoolYear,
            int displayOrder,
            String projectType,
            String creatorCredit,
            MultipartFile screenshot,
            MultipartFile jarFile
    ) throws IOException {

        ShowcaseProject project = projects.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Project not found"));

        setFields(project, title, description, schoolYear,
                displayOrder, projectType, creatorCredit);

        projects.save(project);

        fileService.saveFile(project, "IMAGE", screenshot);
        fileService.saveFile(project, "JAR", jarFile);
    }

    @Transactional
    public void delete(Long id) {

        if (!projects.existsById(id)) {
            throw new NoSuchElementException("Project not found");
        }

        fileService.deleteFiles(id);
        projects.deleteById(id);
    }

    private void setFields(
            ShowcaseProject project,
            String title,
            String description,
            String schoolYear,
            int displayOrder,
            String projectType,
            String creatorCredit) {

        project.title = title.trim();
        project.description = description;
        project.schoolYear = schoolYear;
        project.displayOrder = displayOrder;
        project.projectType = projectType;
        project.creatorCredit = creatorCredit.trim();
    }
}
