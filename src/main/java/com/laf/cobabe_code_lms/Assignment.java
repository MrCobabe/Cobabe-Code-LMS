package com.laf.cobabe_code_lms;
import jakarta.persistence.*;
@Entity public class Assignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=160) public String title;
 @Column(length=12000) public String description;
 @Column(length=12000) public String starterCode;
 public Assignment(){}
}
