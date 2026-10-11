
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public int maxPoints = 100;

    @Column(nullable = false, length = 160)
    public String title;

    @Column(length = 12000)
    public String description;

    @Column(length = 12000)
    public String starterCode;

    @ManyToMany
    @JoinTable(
            name = "assignment_class_periods",
            joinColumns = @JoinColumn(
                    name = "assignment_id"),
            inverseJoinColumns = @JoinColumn(
                    name = "class_period_id")
    )
    public Set<ClassPeriod> classPeriods = new HashSet<>();

    public Assignment() {
    }
}
