package com.sicms.controller;

import com.sicms.dto.StudentImportPreviewResponse;
import com.sicms.dto.StudentImportResultResponse;
import com.sicms.dto.StudentImportRowDto;
import com.sicms.service.StudentImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller dedicated to Faculty Excel Bulk Student Profile Import.
 * Destination section is strictly bound to the authenticated Faculty's active assigned section(s).
 */
@RestController
@RequestMapping("/api/faculty/students/import")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN') or hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN', 'FACULTY', 'ADMIN')")
public class FacultyStudentImportController {

    private final StudentImportService importService;

    @Autowired
    public FacultyStudentImportController(StudentImportService importService) {
        this.importService = importService;
    }

    /**
     * Download official blank Excel template with formatting & sample row.
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String filename = "Student_Registration_Template.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        importService.generateTemplate(response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Step 1: Upload Excel file for validation and preview.
     * Destination is automatically determined by the authenticated faculty's assignment.
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportPreviewResponse> validateImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            Authentication authentication
    ) {
        String facultyEmail = authentication != null ? authentication.getName() : null;
        StudentImportPreviewResponse preview = importService.validateFacultyImport(file, assignmentId, facultyEmail);
        return ResponseEntity.ok(preview);
    }

    /**
     * Step 2: Confirm import and execute transactional batch creation in database.
     * All students are assigned to the authenticated faculty's assigned section.
     */
    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportResultResponse> confirmImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "skipDuplicates", defaultValue = "true") boolean skipDuplicates,
            @RequestParam(value = "updateExisting", defaultValue = "false") boolean updateExisting,
            Authentication authentication
    ) {
        String facultyEmail = authentication != null ? authentication.getName() : null;
        StudentImportResultResponse result = importService.confirmFacultyImport(file, assignmentId, skipDuplicates, updateExisting, facultyEmail);
        return ResponseEntity.ok(result);
    }

    /**
     * Download generated Error Report Excel workbook for failed/skipped rows.
     */
    @PostMapping(value = "/error-report", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void downloadErrorReport(
            @RequestBody List<StudentImportRowDto> failedRows,
            HttpServletResponse response
    ) throws IOException {
        String filename = "Student_Import_Errors.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        importService.generateErrorReport(failedRows, response.getOutputStream());
        response.flushBuffer();
    }
}
