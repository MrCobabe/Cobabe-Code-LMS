package com.laf.cobabe_code_lms.controllers;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.FormatterException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/java")
public class JavaFormatController {

    private final Formatter formatter = new Formatter(
            JavaFormatterOptions.builder()
                    .style(JavaFormatterOptions.Style.AOSP)
                    .build()
    );

    @PostMapping("/format")
    public ResponseEntity<String> formatJava(@RequestBody String code) {
        try {
            return ResponseEntity.ok(
                    formatter.formatSource(code)
            );
        } catch (FormatterException e) {
            return ResponseEntity.badRequest()
                    .body("Java formatting failed: " + e.getMessage());
        }
    }
}