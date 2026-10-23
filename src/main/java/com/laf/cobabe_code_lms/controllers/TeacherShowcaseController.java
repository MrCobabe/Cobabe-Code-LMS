
package com.laf.cobabe_code_lms.controllers;

import com.laf.cobabe_code_lms.models.ShowcaseProject;
import com.laf.cobabe_code_lms.repositories.ShowcaseProjects;
import com.laf.cobabe_code_lms.services.ShowcaseFileService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.laf.cobabe_code_lms.services.ShowcaseProjectService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/teacher/showcase")
public class TeacherShowcaseController {

    private final ShowcaseProjects showcaseProjects;
    private final ShowcaseFileService showcaseFileService;
    private final ShowcaseProjectService showcaseProjectService;

    public TeacherShowcaseController(
            ShowcaseProjects showcaseProjects, ShowcaseFileService showcaseFileService,
            ShowcaseProjectService showcaseProjectService) {

        this.showcaseProjects = showcaseProjects;
        this.showcaseFileService = showcaseFileService;
        this.showcaseProjectService = showcaseProjectService;
    }

    @GetMapping
    public String manage(Model model) {

        List<ShowcaseProject> projects = showcaseProjects
                .findAllByOrderBySchoolYearDescDisplayOrderAsc();

        model.addAttribute("projects", projects);
        model.addAttribute("schoolYears", availableSchoolYears());


        java.util.Map<Long, Boolean> hasImages =
                new java.util.HashMap<>();

        java.util.Map<Long, Boolean> hasJars =
                new java.util.HashMap<>();

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

        model.addAttribute("hasImages", hasImages);
        model.addAttribute("hasJars", hasJars);

        return "teacher-showcase";
    }

    @PostMapping("/add")
    public String add(
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam String schoolYear,
            @RequestParam(defaultValue = "0") int displayOrder,
            @RequestParam(defaultValue = "CLASS") String projectType,
            @RequestParam(defaultValue = "") String creatorCredit,
            @RequestParam(value = "screenshot", required = false)
            MultipartFile screenshot,
            @RequestParam(value = "jarFile", required = false)
            MultipartFile jarFile,
            RedirectAttributes redirectAttributes) {

        if (!validProject(
                title, description, schoolYear,
                projectType, creatorCredit)) {

            return "redirect:/teacher/showcase?invalid";
        }


        try {
            showcaseProjectService.create(
                    title, description, schoolYear,
                    displayOrder, projectType, creatorCredit,
                    screenshot, jarFile);

        } catch (IOException | IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "uploadError", "Upload failed: " + ex.getMessage());

            return "redirect:/teacher/showcase";
        }


        return "redirect:/teacher/showcase?added";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            @RequestParam String schoolYear,
            @RequestParam(defaultValue = "0") int displayOrder,
            @RequestParam(defaultValue = "CLASS") String projectType,
            @RequestParam(defaultValue = "") String creatorCredit,
            @RequestParam(value = "screenshot", required = false)
            MultipartFile screenshot,
            @RequestParam(value = "jarFile", required = false)
            MultipartFile jarFile,
            RedirectAttributes redirectAttributes) {

        if (!validProject(
                title, description, schoolYear,
                projectType, creatorCredit)) {

            return "redirect:/teacher/showcase?invalid";
        }


        try {
            showcaseProjectService.edit(
                    id, title, description, schoolYear,
                    displayOrder, projectType, creatorCredit,
                    screenshot, jarFile);

        } catch (IOException | IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "uploadError", "Upload failed: " + ex.getMessage());

            return "redirect:/teacher/showcase";
        }


        return "redirect:/teacher/showcase?updated";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id) {

        ShowcaseProject project = showcaseProjects
                .findById(id)
                .orElseThrow();

        project.published = true;
        showcaseProjects.save(project);

        return "redirect:/teacher/showcase";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id) {

        ShowcaseProject project = showcaseProjects
                .findById(id)
                .orElseThrow();

        project.published = false;
        showcaseProjects.save(project);

        return "redirect:/teacher/showcase";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {

        showcaseProjectService.delete(id);

        return "redirect:/teacher/showcase";
    }

    private void updateProjectFields(
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

    private boolean validProject(
            String title,
            String description,
            String schoolYear,
            String projectType,
            String creatorCredit) {

        return title != null
                && !title.isBlank()
                && title.length() <= 160
                && description != null
                && description.length() <= 12000
                && schoolYear != null
                && availableSchoolYears().contains(schoolYear)
                && ("CLASS".equals(projectType)
                || "STUDENT".equals(projectType))
                && creatorCredit != null
                && creatorCredit.length() <= 500;
    }

    private List<String> availableSchoolYears() {

        LocalDate today = LocalDate.now();

        int startingYear = today.getMonthValue() >= 8
                ? today.getYear()
                : today.getYear() - 1;

        List<String> years = new ArrayList<>();

        for (int year = startingYear; year >= 2020; year--) {
            years.add(year + "-" + (year + 1));
        }

        return years;
    }
}
