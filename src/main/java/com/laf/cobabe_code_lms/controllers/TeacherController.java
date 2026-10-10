package com.laf.cobabe_code_lms.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherController {

    @GetMapping("/teacher")
    public String teacherDashboard() {
        return "teacher";
    }

}
