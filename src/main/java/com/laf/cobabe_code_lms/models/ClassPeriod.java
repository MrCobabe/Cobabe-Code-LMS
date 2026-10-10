
package com.laf.cobabe_code_lms.models;

import jakarta.persistence.*;

@Entity
@Table(name = "class_periods")
public class ClassPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false)
    public int periodNumber;

    public ClassPeriod() {
    }

    public ClassPeriod(String name, int periodNumber) {
        this.name = name;
        this.periodNumber = periodNumber;
    }
}
