package com.laf.cobabe_code_lms;
import jakarta.persistence.*;
@Entity public class Student {
 @Id public String id;
 @Column(nullable=false) public String email;
 public String name;
 public boolean teacher;
 protected Student(){}
 public Student(String id,String email,String name,boolean teacher){this.id=id;this.email=email;this.name=name;this.teacher=teacher;}
}
