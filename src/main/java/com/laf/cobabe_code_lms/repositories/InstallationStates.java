
package com.laf.cobabe_code_lms.repositories;

import com.laf.cobabe_code_lms.models.InstallationState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationStates
        extends JpaRepository<InstallationState, Long> {
}
