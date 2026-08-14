package com.sicms.controller;

import com.sicms.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StudentExportControllerTest {

    private MockMvc mockMvc;
    private TestableStudentService testableStudentService;
    private StudentExportController controller;

    static class TestableStudentService extends StudentService {
        AtomicBoolean exportCalled = new AtomicBoolean(false);
        AtomicReference<String> passedEmail = new AtomicReference<>();
        AtomicReference<Boolean> passedIsFaculty = new AtomicReference<>();
        AtomicReference<String> passedIp = new AtomicReference<>();

        public TestableStudentService() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public String determineExportFilename(String currentUserEmail, boolean isFaculty) {
            if (!isFaculty) {
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
                return "All_Students_" + today + ".xlsx";
            }
            return "MPC-A_Students.xlsx";
        }

        @Override
        public void exportStudentsToExcel(OutputStream outputStream, String currentUserEmail, boolean isFaculty, String ipAddress) throws IOException {
            exportCalled.set(true);
            passedEmail.set(currentUserEmail);
            passedIsFaculty.set(isFaculty);
            passedIp.set(ipAddress);
            outputStream.write("dummy-excel-bytes".getBytes());
        }
    }

    @BeforeEach
    public void setup() {
        testableStudentService = new TestableStudentService();
        controller = new StudentExportController(testableStudentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Admin Export: Should stream Excel and set All_Students_yyyy_MM_dd.xlsx header")
    public void testAdminExportExcel() throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String expectedFilename = "All_Students_" + today + ".xlsx";

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@college.edu", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/api/students/export/excel").principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expectedFilename + "\""));

        assertTrue(testableStudentService.exportCalled.get());
        assertEquals("admin@college.edu", testableStudentService.passedEmail.get());
        assertFalse(testableStudentService.passedIsFaculty.get());
    }

    @Test
    @DisplayName("Faculty Export: Should stream Excel and set section-based filename MPC-A_Students.xlsx")
    public void testFacultyExportExcel() throws Exception {
        String expectedFilename = "MPC-A_Students.xlsx";

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "faculty@college.edu", "password", List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))
        );

        mockMvc.perform(get("/api/students/export/excel").principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expectedFilename + "\""));

        assertTrue(testableStudentService.exportCalled.get());
        assertEquals("faculty@college.edu", testableStudentService.passedEmail.get());
        assertTrue(testableStudentService.passedIsFaculty.get());
    }
}
