package com.laf.cobabe_code_lms.models;
import jakarta.persistence.*;
import java.time.Instant;
@Entity public class Submission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Assignment assignment;
 @ManyToOne(optional=false) public Student student;
 @Column(nullable=false,length=50000) public String code;
 public Instant submittedAt=Instant.now();
 public Integer grade;
 @Column(length=4000) public String feedback;
 public Submission(){}
}
