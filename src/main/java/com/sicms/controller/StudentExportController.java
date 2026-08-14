package com.sicms.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicms.service.StudentService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/admin/students/export")
public class StudentExportController {

    private final StudentService studentService;

    public StudentExportController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * ADMIN & SUPER_ADMIN ONLY: Export all students directory as Excel (.xlsx) file
     * Response Headers:
     * - Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * - Content-Disposition: attachment; filename="students_directory_yyyy_MM_dd_HH_mm.xlsx"
     */
    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    public void exportStudentsToExcel(HttpServletResponse response) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm"));
        String filename = "students_directory_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        studentService.exportStudentsToExcel(response.getOutputStream());
        response.flushBuffer();
    }
}
