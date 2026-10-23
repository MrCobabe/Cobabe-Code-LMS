
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.ShowcaseProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowcaseProjects
        extends JpaRepository<ShowcaseProject, Long> {

    // Public gallery: published projects only.
    List<ShowcaseProject>
    findByPublishedTrueOrderBySchoolYearDescDisplayOrderAsc();

    // Teacher dashboard: all projects, including drafts.
    List<ShowcaseProject>
    findAllByOrderBySchoolYearDescDisplayOrderAsc();

    // Public gallery filtered to a school year.
    List<ShowcaseProject>
    findBySchoolYearAndPublishedTrueOrderByDisplayOrderAsc(
            String schoolYear
    );
}
