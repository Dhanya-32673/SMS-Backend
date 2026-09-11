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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StudentImportControllerTest {

    private MockMvc mockMvc;
    private TestableStudentImportService testableService;
    private StudentImportController controller;

    static class TestableStudentImportService extends StudentImportService {
        AtomicBoolean templateCalled = new AtomicBoolean(false);
        AtomicBoolean validateCalled = new AtomicBoolean(false);
        AtomicBoolean confirmCalled = new AtomicBoolean(false);
        AtomicBoolean errorReportCalled = new AtomicBoolean(false);

        public TestableStudentImportService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void generateTemplate(OutputStream outputStream) throws IOException {
            templateCalled.set(true);
            outputStream.write("sample-template".getBytes());
        }

        @Override
        public StudentImportPreviewResponse validateImport(MultipartFile file) {
            validateCalled.set(true);
            StudentImportPreviewResponse preview = new StudentImportPreviewResponse();
            preview.setFileName(file.getOriginalFilename());
            preview.setTotalRows(1);
            preview.setValidRows(1);
            preview.setCanProceed(true);
            return preview;
        }

        @Override
        public StudentImportResultResponse confirmImport(MultipartFile file, boolean skipDuplicates, boolean updateExisting, String currentUserEmail) {
            confirmCalled.set(true);
            StudentImportResultResponse res = new StudentImportResultResponse();
            res.setTotalRows(5);
            res.setImportedCount(5);
            res.setMessage("Import completed: 5 created.");
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
        testableService = new TestableStudentImportService();
        controller = new StudentImportController(testableService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Download Template: Streams Excel with Content-Disposition")
    public void testDownloadTemplate() throws Exception {
        mockMvc.perform(get("/api/admin/students/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Student_Registration_Template.xlsx\""));

        assertTrue(testableService.templateCalled.get());
    }

    @Test
    @DisplayName("Validate Import: Parses uploaded Excel and returns preview response")
    public void testValidateImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-data".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/students/import/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("students.xlsx"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.validRows").value(1))
                .andExpect(jsonPath("$.canProceed").value(true));

        assertTrue(testableService.validateCalled.get());
    }

    @Test
    @DisplayName("Confirm Import: Executes batch creation and returns result summary")
    public void testConfirmImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-data".getBytes()
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@college.edu", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(multipart("/api/admin/students/import/confirm")
                        .file(file)
                        .param("skipDuplicates", "true")
                        .param("updateExisting", "false")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(5))
                .andExpect(jsonPath("$.importedCount").value(5));

        assertTrue(testableService.confirmCalled.get());
    }

    @Test
    @DisplayName("Download Error Report: Streams error Excel with Content-Disposition")
    public void testDownloadErrorReport() throws Exception {
        mockMvc.perform(post("/api/admin/students/import/error-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Student_Import_Errors.xlsx\""));

        assertTrue(testableService.errorReportCalled.get());
    }
}
