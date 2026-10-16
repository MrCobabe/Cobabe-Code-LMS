
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(name = "installation_state")
public class InstallationState {

    @Id
    public Long id = 1L;

    @Column(nullable = false)
    public boolean teacherInitialized = false;
}
