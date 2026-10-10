package com.laf.cobabe_code_lms;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface Students extends JpaRepository<Student,String>{}
interface Assignments extends JpaRepository<Assignment,Long>{}
interface Submissions extends JpaRepository<Submission,Long>{
 List<Submission> findByStudentIdOrderBySubmittedAtDesc(String studentId);
 List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
}
