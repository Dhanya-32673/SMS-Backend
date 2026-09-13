package com.sicms.controller;

import com.sicms.dto.StudentImportPreviewResponse;
import com.sicms.dto.StudentImportResultResponse;
import com.sicms.dto.StudentImportRowDto;
import com.sicms.service.StudentImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FacultyStudentImportControllerTest {

    private MockMvc mockMvc;
    private TestableFacultyStudentImportService testableService;
    private FacultyStudentImportController controller;

    static class TestableFacultyStudentImportService extends StudentImportService {
        AtomicBoolean templateCalled = new AtomicBoolean(false);
        AtomicBoolean validateCalled = new AtomicBoolean(false);
        AtomicBoolean confirmCalled = new AtomicBoolean(false);
        AtomicBoolean errorReportCalled = new AtomicBoolean(false);

        public TestableFacultyStudentImportService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void generateTemplate(OutputStream outputStream) throws IOException {
            templateCalled.set(true);
            outputStream.write("sample-template".getBytes());
        }

        @Override
        public StudentImportPreviewResponse validateFacultyImport(MultipartFile file, Long assignmentId, String facultyEmail) {
            validateCalled.set(true);
            StudentImportPreviewResponse preview = new StudentImportPreviewResponse();
            preview.setFileName(file.getOriginalFilename());
            preview.setTotalRows(1);
            preview.setValidRows(1);
            preview.setCanProceed(true);
            preview.setTargetGroup("MPC");
            preview.setTargetYear("1st Year");
            preview.setTargetSection("Section B");
            preview.setRole("ROLE_FACULTY");
            return preview;
        }

        @Override
        public StudentImportResultResponse confirmFacultyImport(MultipartFile file, Long assignmentId, boolean skipDuplicates, boolean updateExisting, String facultyEmail) {
            confirmCalled.set(true);
            StudentImportResultResponse res = new StudentImportResultResponse();
            res.setTotalRows(1);
            res.setImportedCount(1);
            res.setTargetGroup("MPC");
            res.setTargetYear("1st Year");
            res.setTargetSection("Section B");
            res.setCreatorRole("ROLE_FACULTY");
            res.setMessage("Import completed: 1 created.");
            return res;
        }

        @Override
        public void generateErrorReport(List<StudentImportRowDto> failedRows, OutputStream outputStream) throws IOException {
            errorReportCalled.set(true);
            outputStream.write("sample-error-report".getBytes());
        }
    }

    @BeforeEach
    public void setup() {
        testableService = new TestableFacultyStudentImportService();
        controller = new FacultyStudentImportController(testableService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Download Template: Streams Excel with Content-Disposition for Faculty")
    public void testDownloadTemplate() throws Exception {
        mockMvc.perform(get("/api/faculty/students/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Student_Registration_Template.xlsx\""));

        assertTrue(testableService.templateCalled.get());
    }

    @Test
    @DisplayName("Validate Faculty Import: Binds authenticated faculty assignment")
    public void testValidateFacultyImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faculty_students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-data".getBytes()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "faculty@college.edu",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))
        );

        mockMvc.perform(multipart("/api/faculty/students/import/validate")
                        .file(file)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("faculty_students.xlsx"))
                .andExpect(jsonPath("$.targetGroup").value("MPC"))
                .andExpect(jsonPath("$.targetYear").value("1st Year"))
                .andExpect(jsonPath("$.targetSection").value("Section B"))
                .andExpect(jsonPath("$.role").value("ROLE_FACULTY"));

        assertTrue(testableService.validateCalled.get());
    }

    @Test
    @DisplayName("Confirm Faculty Import: Creates students in faculty assigned section")
    public void testConfirmFacultyImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faculty_students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-data".getBytes()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "faculty@college.edu",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))
        );

        mockMvc.perform(multipart("/api/faculty/students/import/confirm")
                        .file(file)
                        .param("skipDuplicates", "true")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.targetGroup").value("MPC"))
                .andExpect(jsonPath("$.targetSection").value("Section B"))
                .andExpect(jsonPath("$.creatorRole").value("ROLE_FACULTY"));

        assertTrue(testableService.confirmCalled.get());
    }

    @Test
    @DisplayName("Error Report: Streams error report workbook")
    public void testErrorReport() throws Exception {
        String jsonPayload = "[{\"rowNumber\":2,\"studentId\":\"STU999\",\"fullName\":\"Test Student\",\"errors\":[\"Invalid Email\"]}]";

        mockMvc.perform(post("/api/faculty/students/import/error-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Student_Import_Errors.xlsx\""));

        assertTrue(testableService.errorReportCalled.get());
    }
}
