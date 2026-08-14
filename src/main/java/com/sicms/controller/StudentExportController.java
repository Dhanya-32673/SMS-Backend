package com.sicms.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicms.service.StudentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping({"/api/students/export", "/api/admin/students/export"})
public class StudentExportController {

    private final StudentService studentService;

    public StudentExportController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * Export permitted students directory as Excel (.xlsx) file
     * - ADMIN / SUPER_ADMIN: Exports all students (All_Students_yyyy_MM_dd.xlsx)
     * - FACULTY: Exports only assigned section students & self-created students (e.g. MPC-A_Students.xlsx or MPC-A_MPC-B_Students.xlsx)
     * Response Headers:
     * - Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * - Content-Disposition: attachment; filename="..."
     */
    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_FACULTY', 'ADMIN', 'SUPER_ADMIN', 'FACULTY')")
    public void exportStudentsToExcel(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String email = authentication != null ? authentication.getName() : null;
        boolean isFaculty = isFacultyRole(authentication);

        String filename = studentService.determineExportFilename(email, isFaculty);
        String clientIp = extractClientIp(request);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        studentService.exportStudentsToExcel(response.getOutputStream(), email, isFaculty, clientIp);
        response.flushBuffer();
    }

    private boolean isFacultyRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        boolean hasAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_ADMIN") || a.equalsIgnoreCase("ROLE_SUPER_ADMIN")
                        || a.equalsIgnoreCase("ADMIN") || a.equalsIgnoreCase("SUPER_ADMIN"));

        if (hasAdmin) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_FACULTY") || a.equalsIgnoreCase("FACULTY"));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
