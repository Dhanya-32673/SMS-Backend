package com.sicms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.repository.DocumentTypeRepository;
import com.sicms.repository.FacultyRepository;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "supabase.url=",
                "spring.mail.host=localhost",
                "spring.mail.port=2525"
        }
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SicmsApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @SuppressWarnings("unused")
    private FacultyRepository facultyRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private OtpRepository otpRepository;

    private String adminAccessToken;
    private String adminRefreshToken;
    private String facultyAccessToken;


    private Long createdFacultyId;
    private Long createdFacultyUserId;
    private String createdFacultyEmail;
    private String createdFacultyPassword = "TempPass123!";

    private Long createdSectionId;
    private String createdStudentId;
    private Long createdDocumentTypeId;
    private Long uploadedDocumentId;
    private Long deletableDocumentId;

    @BeforeAll
    void setUp() throws Exception {
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient())
        );

        JsonNode adminLogin = login("admin@college.edu", "AdminPass123!");
        adminAccessToken = adminLogin.get("accessToken").asText();
        adminRefreshToken = adminLogin.get("refreshToken").asText();

        JsonNode facultyLogin = login("2400032673cse1@gmail.com", "FacultyPass123!");
        facultyAccessToken = facultyLogin.get("accessToken").asText();


    }

    @Test
    @Order(1)
    void authenticationAndSecurityEndpointsWork() throws Exception {
        ResponseEntity<String> invalidLogin = postJson("/api/auth/login",
                Map.of("email", "admin@college.edu", "password", "wrong-password"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, invalidLogin.getStatusCode());

        ResponseEntity<String> googleMissingToken = postJson("/api/auth/google", Map.of(), null);
        assertEquals(HttpStatus.BAD_REQUEST, googleMissingToken.getStatusCode());

        ResponseEntity<String> otpSend = postJson("/api/auth/otp/send",
                Map.of("email", "admin@college.edu", "purpose", "LOGIN"), null);
        assertEquals(HttpStatus.OK, otpSend.getStatusCode());

        String loginOtp = waitForOtp("admin@college.edu", "LOGIN");
        ResponseEntity<String> otpVerify = postJson("/api/auth/otp/verify",
                Map.of("email", "admin@college.edu", "otp", loginOtp, "purpose", "LOGIN"), null);
        assertEquals(HttpStatus.OK, otpVerify.getStatusCode());
        assertNotNull(readJson(otpVerify).get("accessToken").asText());

        ResponseEntity<String> refresh = postJson("/api/auth/refresh",
                Map.of("refreshToken", adminRefreshToken), null);
        assertEquals(HttpStatus.OK, refresh.getStatusCode());
        JsonNode refreshJson = readJson(refresh);
        adminAccessToken = refreshJson.get("accessToken").asText();
        adminRefreshToken = refreshJson.get("refreshToken").asText();

        ResponseEntity<String> logout = postJson("/api/auth/logout",
                Map.of("refreshToken", adminRefreshToken), null);
        assertEquals(HttpStatus.OK, logout.getStatusCode());

        JsonNode relogin = login("admin@college.edu", "AdminPass123!");
        adminAccessToken = relogin.get("accessToken").asText();
        adminRefreshToken = relogin.get("refreshToken").asText();

        ResponseEntity<String> me = exchange("/api/auth/me", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, me.getStatusCode());
        assertEquals("admin@college.edu", readJson(me).get("email").asText());

        ResponseEntity<String> meAnonymous = exchange("/api/auth/me", HttpMethod.GET, null, null, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.UNAUTHORIZED, meAnonymous.getStatusCode());

        ResponseEntity<String> studentsAnonymous = exchange("/api/students", HttpMethod.GET, null, null, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.UNAUTHORIZED, studentsAnonymous.getStatusCode());

        ResponseEntity<String> adminTest = exchange("/api/admin/test", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, adminTest.getStatusCode());

        ResponseEntity<String> adminTestFaculty = exchange("/api/admin/test", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.FORBIDDEN, adminTestFaculty.getStatusCode());

        ResponseEntity<String> facultyTest = exchange("/api/faculty/test", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, facultyTest.getStatusCode());
    }

    @Test
    @Order(2)
    void academicAndDocumentTypeEndpointsWork() throws Exception {
        ResponseEntity<String> groups = exchange("/api/academic/groups", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, groups.getStatusCode());

        String groupCode = "QA" + System.currentTimeMillis();
        ResponseEntity<String> createGroup = postJson("/api/academic/groups", Map.of(
                "code", groupCode,
                "name", "Quality Assurance " + groupCode,
                "description", "Created by automated API test",
                "active", true
        ), adminAccessToken);
        assertEquals(HttpStatus.CREATED, createGroup.getStatusCode());

        ResponseEntity<String> sections = exchange("/api/academic/sections", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, sections.getStatusCode());

        ResponseEntity<String> createSection = postJson("/api/academic/sections", Map.of(
                "name", "Z",
                "academicYear", "2026-2027",
                "branchGroup", "MPC",
                "intermediateYear", "1st Year",
                "capacity", 25,
                "description", "Automation test section",
                "active", true
        ), adminAccessToken);
        assertEquals(HttpStatus.CREATED, createSection.getStatusCode());
        createdSectionId = readJson(createSection).get("id").asLong();

        ResponseEntity<String> updateSection = jsonRequest(
                "/api/academic/sections/" + createdSectionId,
                HttpMethod.PUT,
                Map.of(
                        "name", "Z1",
                        "academicYear", "2026-2027",
                        "branchGroup", "MPC",
                        "intermediateYear", "1st Year",
                        "capacity", 30,
                        "description", "Updated by automation",
                        "active", true
                ),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, updateSection.getStatusCode());

        createdDocumentTypeId = documentTypeRepository.findByCodeIgnoreCase("SSC_MEMO")
                .orElseThrow()
                .getId();

        ResponseEntity<String> documentTypes = exchange("/api/document-types", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, documentTypes.getStatusCode());

        ResponseEntity<String> activeDocumentTypes = exchange("/api/document-types/active", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, activeDocumentTypes.getStatusCode());

        String typeCode = "AUTO_DOC_" + System.currentTimeMillis();
        ResponseEntity<String> createDocumentType = postJson("/api/document-types", Map.of(
                "code", typeCode,
                "name", "Automation Certificate",
                "category", "ACADEMIC",
                "description", "Created by integration tests",
                "requiredByDefault", false,
                "hasExpiry", false,
                "active", true
        ), adminAccessToken);
        assertEquals(HttpStatus.CREATED, createDocumentType.getStatusCode());
        createdDocumentTypeId = readJson(createDocumentType).get("id").asLong();

        ResponseEntity<String> updateDocumentType = jsonRequest(
                "/api/document-types/" + createdDocumentTypeId,
                HttpMethod.PUT,
                Map.of(
                        "code", typeCode,
                        "name", "Automation Certificate Updated",
                        "category", "IDENTITY",
                        "description", "Updated by integration tests",
                        "requiredByDefault", true,
                        "hasExpiry", true,
                        "active", true
                ),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, updateDocumentType.getStatusCode());
    }

    @Test
    @Order(3)
    void facultyEndpointsAndPasswordFlowsWork() throws Exception {
        ResponseEntity<String> facultyList = exchange("/api/faculty", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, facultyList.getStatusCode());

        createdFacultyEmail = "autofaculty" + System.currentTimeMillis() + "@example.com";
        ResponseEntity<String> createFaculty = postJson("/api/faculty", Map.of(
                "firstName", "Auto",
                "lastName", "Faculty",
                "gender", "MALE",
                "mobileNumber", "9876501234",
                "email", createdFacultyEmail,
                "designation", "Lecturer",
                "department", "General Sciences",
                "primaryGroup", "MPC",
                "password", createdFacultyPassword,
                "joiningDate", "2024-06-01"
        ), adminAccessToken);
        assertEquals(HttpStatus.CREATED, createFaculty.getStatusCode());
        JsonNode facultyJson = readJson(createFaculty);
        createdFacultyId = facultyJson.get("id").asLong();
        createdFacultyUserId = facultyJson.get("userId").asLong();

        ResponseEntity<String> getFaculty = exchange("/api/faculty/" + createdFacultyId, HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, getFaculty.getStatusCode());

        ResponseEntity<String> updateFaculty = jsonRequest(
                "/api/faculty/" + createdFacultyId,
                HttpMethod.PUT,
                Map.ofEntries(
                        Map.entry("firstName", "Auto"),
                        Map.entry("middleName", "QA"),
                        Map.entry("lastName", "Faculty"),
                        Map.entry("gender", "FEMALE"),
                        Map.entry("mobileNumber", "9876501235"),
                        Map.entry("email", createdFacultyEmail),
                        Map.entry("designation", "Senior Lecturer"),
                        Map.entry("department", "Physics"),
                        Map.entry("primaryGroup", "MPC"),
                        Map.entry("joiningDate", "2024-06-02"),
                        Map.entry("employmentType", "PERMANENT"),
                        Map.entry("status", "ACTIVE")
                ),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, updateFaculty.getStatusCode());

        ResponseEntity<String> addAssignment = postJson(
                "/api/faculty/" + createdFacultyId + "/assignments",
                Map.of(
                        "branchGroup", "MPC",
                        "intermediateYear", "1st Year",
                        "section", "Z1",
                        "academicYear", "2026-2027",
                        "subjectName", "Automation Testing"
                ),
                adminAccessToken
        );
        assertEquals(HttpStatus.CREATED, addAssignment.getStatusCode());

        ResponseEntity<String> getAssignments = exchange("/api/faculty/" + createdFacultyId + "/assignments", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, getAssignments.getStatusCode());

        ResponseEntity<String> disableFaculty = jsonRequest(
                "/api/faculty/" + createdFacultyId + "/status",
                HttpMethod.PATCH,
                Map.of("status", "INACTIVE"),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, disableFaculty.getStatusCode());

        ResponseEntity<String> disabledLogin = postJson("/api/auth/login",
                Map.of("email", createdFacultyEmail, "password", createdFacultyPassword), null);
        assertEquals(HttpStatus.FORBIDDEN, disabledLogin.getStatusCode());

        ResponseEntity<String> enableFaculty = jsonRequest(
                "/api/faculty/" + createdFacultyId + "/status",
                HttpMethod.PATCH,
                Map.of("status", "ACTIVE"),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, enableFaculty.getStatusCode());

        JsonNode facultyLogin = login(createdFacultyEmail, createdFacultyPassword);
        String createdFacultyAccessToken = facultyLogin.get("accessToken").asText();

        ResponseEntity<String> currentAssignments = exchange("/api/faculty/me/assignments", HttpMethod.GET, null, createdFacultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, currentAssignments.getStatusCode());

        String profilePassword = "ProfilePass123!";
        ResponseEntity<String> profileChangePassword = jsonRequest(
                "/api/profile/change-password",
                HttpMethod.PUT,
                Map.of(
                        "currentPassword", createdFacultyPassword,
                        "newPassword", profilePassword,
                        "confirmPassword", profilePassword
                ),
                createdFacultyAccessToken
        );
        assertEquals(HttpStatus.OK, profileChangePassword.getStatusCode());
        createdFacultyPassword = profilePassword;

        facultyLogin = login(createdFacultyEmail, createdFacultyPassword);
        createdFacultyAccessToken = facultyLogin.get("accessToken").asText();

        String authPassword = "AuthPass123!";
        ResponseEntity<String> authChangePassword = jsonRequest(
                "/api/auth/change-password",
                HttpMethod.PUT,
                Map.of(
                        "currentPassword", createdFacultyPassword,
                        "newPassword", authPassword,
                        "confirmPassword", authPassword
                ),
                createdFacultyAccessToken
        );
        assertEquals(HttpStatus.OK, authChangePassword.getStatusCode());
        createdFacultyPassword = authPassword;

        facultyLogin = login(createdFacultyEmail, createdFacultyPassword);
        createdFacultyAccessToken = facultyLogin.get("accessToken").asText();

        ResponseEntity<String> forgotPassword = postJson("/api/auth/forgot-password",
                Map.of("email", createdFacultyEmail), null);
        assertEquals(HttpStatus.OK, forgotPassword.getStatusCode());

        String resetOtp = waitForOtp(createdFacultyEmail, "PASSWORD_RESET");
        String resetPassword = "ResetPass123!";
        ResponseEntity<String> resetPasswordResponse = postJson("/api/auth/reset-password",
                Map.of(
                        "email", createdFacultyEmail,
                        "otp", resetOtp,
                        "newPassword", resetPassword,
                        "confirmPassword", resetPassword
                ), null);
        assertEquals(HttpStatus.OK, resetPasswordResponse.getStatusCode());
        createdFacultyPassword = resetPassword;

        facultyLogin = login(createdFacultyEmail, createdFacultyPassword);
        createdFacultyAccessToken = facultyLogin.get("accessToken").asText();

        String adminResetPassword = "FinalPass123!";
        ResponseEntity<String> adminReset = postJson(
                "/api/auth/admin/users/" + createdFacultyUserId + "/reset-password",
                Map.of("newPassword", adminResetPassword),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, adminReset.getStatusCode());
        createdFacultyPassword = adminResetPassword;

        facultyLogin = login(createdFacultyEmail, createdFacultyPassword);
        assertNotNull(facultyLogin.get("accessToken").asText());
    }

    @Test
    @Order(4)
    void studentEndpointsWork() throws Exception {
        ResponseEntity<String> invalidStudentCreate = postJson("/api/students", Map.ofEntries(
                Map.entry("rollNumber", "AUTO-INVALID-" + System.currentTimeMillis()),
                Map.entry("lastName", "Student"),
                Map.entry("gender", "MALE"),
                Map.entry("dateOfBirth", "2008-01-01"),
                Map.entry("mobileNumber", "9999999999"),
                Map.entry("email", "invalid@example.com"),
                Map.entry("address", "Test address"),
                Map.entry("fatherName", "Father"),
                Map.entry("motherName", "Mother"),
                Map.entry("parentMobile", "8888888888"),
                Map.entry("branchGroup", "MPC"),
                Map.entry("intermediateYear", "1st Year"),
                Map.entry("section", "Z1"),
                Map.entry("batch", "2026-2028"),
                Map.entry("academicYear", "2026-2027"),
                Map.entry("admissionDate", "2026-06-01"),
                Map.entry("hostelDayScholar", "DAY_SCHOLAR")
        ), adminAccessToken);
        assertEquals(HttpStatus.BAD_REQUEST, invalidStudentCreate.getStatusCode());

        String rollNumber = "AUTO-" + System.currentTimeMillis();
        ResponseEntity<String> createStudent = postJson("/api/students", Map.ofEntries(
                Map.entry("rollNumber", rollNumber),
                Map.entry("firstName", "Auto"),
                Map.entry("lastName", "Student"),
                Map.entry("gender", "MALE"),
                Map.entry("dateOfBirth", "2008-01-01"),
                Map.entry("mobileNumber", "9999999999"),
                Map.entry("email", "autostudent" + System.currentTimeMillis() + "@example.com"),
                Map.entry("address", "Test address"),
                Map.entry("fatherName", "Father"),
                Map.entry("motherName", "Mother"),
                Map.entry("parentMobile", "8888888888"),
                Map.entry("branchGroup", "MPC"),
                Map.entry("intermediateYear", "1st Year"),
                Map.entry("section", "Z1"),
                Map.entry("batch", "2026-2028"),
                Map.entry("academicYear", "2026-2027"),
                Map.entry("admissionDate", "2026-06-01"),
                Map.entry("hostelDayScholar", "DAY_SCHOLAR")
        ), adminAccessToken);
        assertEquals(HttpStatus.CREATED, createStudent.getStatusCode(), createStudent.getBody());
        createdStudentId = readJson(createStudent).get("studentId").asText();

        ResponseEntity<String> duplicateStudent = postJson("/api/students", Map.ofEntries(
                Map.entry("rollNumber", rollNumber),
                Map.entry("firstName", "Auto"),
                Map.entry("lastName", "Duplicate"),
                Map.entry("gender", "MALE"),
                Map.entry("dateOfBirth", "2008-01-02"),
                Map.entry("mobileNumber", "9999999998"),
                Map.entry("email", "duplicate" + System.currentTimeMillis() + "@example.com"),
                Map.entry("address", "Test address"),
                Map.entry("fatherName", "Father"),
                Map.entry("motherName", "Mother"),
                Map.entry("parentMobile", "8888888887"),
                Map.entry("branchGroup", "MPC"),
                Map.entry("intermediateYear", "1st Year"),
                Map.entry("section", "Z1"),
                Map.entry("batch", "2026-2028"),
                Map.entry("academicYear", "2026-2027"),
                Map.entry("admissionDate", "2026-06-01"),
                Map.entry("hostelDayScholar", "DAY_SCHOLAR")
        ), adminAccessToken);
        assertEquals(HttpStatus.CONFLICT, duplicateStudent.getStatusCode());

        ResponseEntity<String> students = exchange("/api/students", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, students.getStatusCode());

        ResponseEntity<String> searchStudents = exchange("/api/students/search?query=Auto", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, searchStudents.getStatusCode());

        ResponseEntity<String> getStudent = exchange("/api/students/" + createdStudentId, HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, getStudent.getStatusCode());

        ResponseEntity<String> updateStudent = jsonRequest(
                "/api/students/" + createdStudentId,
                HttpMethod.PUT,
                Map.of(
                        "firstName", "Updated",
                        "lastName", "Student",
                        "fullName", "Updated Student",
                        "gender", "FEMALE",
                        "dateOfBirth", "2008-02-02",
                        "status", "ACTIVE"
                ),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, updateStudent.getStatusCode());
        assertEquals("Updated", readJson(updateStudent).get("firstName").asText());

        ResponseEntity<String> uploadPhoto = multipartRequest(
                "/api/students/" + createdStudentId + "/photo",
                HttpMethod.POST,
                Map.of("file", namedResource("avatar.png", "fakepng".getBytes(StandardCharsets.UTF_8))),
                adminAccessToken,
                MediaType.IMAGE_PNG
        );
        assertEquals(HttpStatus.OK, uploadPhoto.getStatusCode());

        ResponseEntity<String> invalidPhoto = multipartRequest(
                "/api/students/" + createdStudentId + "/photo",
                HttpMethod.POST,
                Map.of("file", namedResource("avatar.txt", "plain text".getBytes(StandardCharsets.UTF_8))),
                adminAccessToken,
                MediaType.TEXT_PLAIN
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidPhoto.getStatusCode());

        ResponseEntity<String> idCard = exchange("/api/students/" + createdStudentId + "/id-card", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, idCard.getStatusCode());

        ResponseEntity<String> facultyAllowedStudent = exchange("/api/students/STU2026001001", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, facultyAllowedStudent.getStatusCode());

        ResponseEntity<String> facultyForbiddenStudent = exchange("/api/students/STU2026001004", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.FORBIDDEN, facultyForbiddenStudent.getStatusCode());
    }

    @Test
    @Order(5)
    void documentEndpointsWork() throws Exception {
        ResponseEntity<String> documentList = exchange("/api/documents", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, documentList.getStatusCode());

        ResponseEntity<String> uploadInvalidDocument = multipartRequest(
                "/api/documents",
                HttpMethod.POST,
                Map.of(
                        "studentId", createdStudentId,
                        "documentTypeId", createdDocumentTypeId.toString(),
                        "file", namedResource("bad.txt", "not a pdf".getBytes(StandardCharsets.UTF_8))
                ),
                adminAccessToken,
                MediaType.TEXT_PLAIN
        );
        assertEquals(HttpStatus.BAD_REQUEST, uploadInvalidDocument.getStatusCode());

        ResponseEntity<String> uploadDocument = multipartRequest(
                "/api/documents",
                HttpMethod.POST,
                Map.of(
                        "studentId", createdStudentId,
                        "documentTypeId", createdDocumentTypeId.toString(),
                        "documentNumber", "AUTO-DOC-1",
                        "issueDate", "2026-06-10",
                        "issuedBy", "Automation",
                        "notes", "Initial upload",
                        "file", namedResource("document.pdf", pdfBytes("Automation upload v1"))
                ),
                adminAccessToken,
                MediaType.APPLICATION_PDF
        );
        assertEquals(HttpStatus.CREATED, uploadDocument.getStatusCode());
        uploadedDocumentId = readJson(uploadDocument).get("id").asLong();

        ResponseEntity<String> duplicateUpload = multipartRequest(
                "/api/documents",
                HttpMethod.POST,
                Map.of(
                        "studentId", createdStudentId,
                        "documentTypeId", createdDocumentTypeId.toString(),
                        "documentNumber", "AUTO-DOC-1",
                        "file", namedResource("duplicate.pdf", pdfBytes("Duplicate upload"))
                ),
                adminAccessToken,
                MediaType.APPLICATION_PDF
        );
        assertEquals(HttpStatus.CONFLICT, duplicateUpload.getStatusCode());

        Long seededDocTypeId = documentTypeRepository.findByCodeIgnoreCase("AADHAAR_DOC")
                .orElseThrow()
                .getId();
        ResponseEntity<String> uploadSecondDocument = multipartRequest(
                "/api/documents",
                HttpMethod.POST,
                Map.of(
                        "studentId", createdStudentId,
                        "documentTypeId", seededDocTypeId.toString(),
                        "documentNumber", "AUTO-DOC-2",
                        "file", namedResource("document-2.pdf", pdfBytes("Automation upload v2"))
                ),
                adminAccessToken,
                MediaType.APPLICATION_PDF
        );
        assertEquals(HttpStatus.CREATED, uploadSecondDocument.getStatusCode());
        deletableDocumentId = readJson(uploadSecondDocument).get("id").asLong();

        ResponseEntity<String> getDocument = exchange("/api/documents/" + uploadedDocumentId, HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, getDocument.getStatusCode());

        ResponseEntity<String> documentsByStudent = exchange("/api/documents/student/" + createdStudentId, HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, documentsByStudent.getStatusCode());

        ResponseEntity<String> facultyForbiddenDocuments = exchange("/api/documents/student/STU2026001004", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.FORBIDDEN, facultyForbiddenDocuments.getStatusCode());

        ResponseEntity<String> studentSummariesAdmin = exchange("/api/documents/student-summaries", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, studentSummariesAdmin.getStatusCode());

        ResponseEntity<String> studentSummariesFaculty = exchange("/api/documents/student-summaries", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, studentSummariesFaculty.getStatusCode());

        ResponseEntity<String> missingDocuments = exchange("/api/documents/missing", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, missingDocuments.getStatusCode());

        ResponseEntity<String> replaceDocument = multipartRequest(
                "/api/documents/" + uploadedDocumentId + "/replace",
                HttpMethod.POST,
                Map.of("file", namedResource("replacement.pdf", pdfBytes("Automation replacement"))),
                adminAccessToken,
                MediaType.APPLICATION_PDF
        );
        assertEquals(HttpStatus.OK, replaceDocument.getStatusCode());

        ResponseEntity<byte[]> previewDocument = exchangeForBytes("/api/documents/" + uploadedDocumentId + "/file", HttpMethod.GET, null, adminAccessToken);
        assertEquals(HttpStatus.OK, previewDocument.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, previewDocument.getHeaders().getContentType());
        assertTrue(new String(previewDocument.getBody(), StandardCharsets.ISO_8859_1).contains("%PDF-"));

        ResponseEntity<byte[]> downloadDocument = exchangeForBytes("/api/documents/" + uploadedDocumentId + "/file?download=true", HttpMethod.GET, null, adminAccessToken);
        assertEquals(HttpStatus.OK, downloadDocument.getStatusCode());
        assertTrue(downloadDocument.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        ResponseEntity<String> verifyDocument = jsonRequest("/api/documents/" + uploadedDocumentId + "/verify", HttpMethod.PATCH, null, adminAccessToken);
        assertEquals(HttpStatus.OK, verifyDocument.getStatusCode());
        assertEquals("VERIFIED", readJson(verifyDocument).get("status").asText());

        ResponseEntity<String> rejectDocument = jsonRequest(
                "/api/documents/" + uploadedDocumentId + "/reject",
                HttpMethod.PATCH,
                Map.of("reason", "Automation rejection flow"),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, rejectDocument.getStatusCode());
        assertEquals("REJECTED", readJson(rejectDocument).get("status").asText());

        ResponseEntity<String> archiveDocument = jsonRequest("/api/documents/" + uploadedDocumentId + "/archive", HttpMethod.PATCH, null, adminAccessToken);
        assertEquals(HttpStatus.OK, archiveDocument.getStatusCode());
        assertEquals("ARCHIVED", readJson(archiveDocument).get("status").asText());

        ResponseEntity<String> deleteDocument = exchange("/api/documents/" + deletableDocumentId, HttpMethod.DELETE, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.NO_CONTENT, deleteDocument.getStatusCode());
    }

    @Test
    @Order(6)
    void profileDashboardSectionMembershipAndCleanupWork() throws Exception {
        ResponseEntity<String> profile = exchange("/api/profile", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, profile.getStatusCode());
        assertEquals("admin@college.edu", readJson(profile).get("email").asText());

        ResponseEntity<String> adminDashboard = exchange("/api/dashboard/admin/summary", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, adminDashboard.getStatusCode());

        ResponseEntity<String> facultyDashboard = exchange("/api/dashboard/faculty/summary", HttpMethod.GET, null, facultyAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, facultyDashboard.getStatusCode());

        ResponseEntity<String> initialMembers = exchange("/api/academic/sections/" + createdSectionId + "/members", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, initialMembers.getStatusCode());

        ResponseEntity<String> assignStudent = postJson(
                "/api/academic/sections/" + createdSectionId + "/assign",
                Map.of("studentIds", java.util.List.of(createdStudentId)),
                adminAccessToken
        );
        assertEquals(HttpStatus.OK, assignStudent.getStatusCode());

        ResponseEntity<String> membersAfterAssign = exchange("/api/academic/sections/" + createdSectionId + "/members", HttpMethod.GET, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.OK, membersAfterAssign.getStatusCode());
        assertTrue(membersAfterAssign.getBody().contains(createdStudentId));

        ResponseEntity<String> removeStudent = exchange(
                "/api/academic/sections/" + createdSectionId + "/members/" + createdStudentId,
                HttpMethod.DELETE,
                null,
                adminAccessToken,
                MediaType.APPLICATION_JSON
        );
        assertEquals(HttpStatus.NO_CONTENT, removeStudent.getStatusCode());

        ResponseEntity<String> deactivateStudent = jsonRequest("/api/students/" + createdStudentId + "/deactivate", HttpMethod.PATCH, null, adminAccessToken);
        assertEquals(HttpStatus.NO_CONTENT, deactivateStudent.getStatusCode());

        ResponseEntity<String> deleteStudent = exchange("/api/students/" + createdStudentId, HttpMethod.DELETE, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.NO_CONTENT, deleteStudent.getStatusCode());

        ResponseEntity<String> deleteSection = exchange("/api/academic/sections/" + createdSectionId, HttpMethod.DELETE, null, adminAccessToken, MediaType.APPLICATION_JSON);
        assertEquals(HttpStatus.NO_CONTENT, deleteSection.getStatusCode());
    }

    private JsonNode login(String email, String password) throws Exception {
        ResponseEntity<String> response = postJson("/api/auth/login", Map.of("email", email, "password", password), null);
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());

        String otp = waitForOtp(email, "LOGIN");
        ResponseEntity<String> verifyResponse = postJson("/api/auth/verify-otp", Map.of("email", email, "otp", otp), null);
        assertEquals(HttpStatus.OK, verifyResponse.getStatusCode(), verifyResponse.getBody());

        return readJson(verifyResponse);
    }

    private String waitForOtp(String email, String purpose) throws InterruptedException {
        OtpPurpose otpPurpose = OtpPurpose.valueOf(purpose.trim().toUpperCase());
        Long userId = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow()
                .getId();
        for (int i = 0; i < 20; i++) {
            Optional<OtpVerification> verification = otpRepository.findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    userRepository.findById(userId).orElseThrow(),
                    otpPurpose
            );
            if (verification.isPresent()) {
                return crackOtp(verification.get().getOtpHash());
            }
            Thread.sleep(100);
        }
        fail("OTP not found for " + email + " / " + purpose);
        return null;
    }

    private String crackOtp(String otpHash) {
        for (int i = 0; i <= 999999; i++) {
            if (i <= 9999) {
                String candidate4 = String.format("%04d", i);
                if (hashOtp(candidate4).equals(otpHash)) {
                    return candidate4;
                }
            }
            String candidate6 = String.format("%06d", i);
            if (hashOtp(candidate6).equals(otpHash)) {
                return candidate6;
            }
        }
        fail("Unable to crack OTP hash for automated verification");
        return null;
    }

    private String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private ResponseEntity<String> postJson(String path, Object body, String bearerToken) {
        return jsonRequest(path, HttpMethod.POST, body, bearerToken);
    }

    private ResponseEntity<String> jsonRequest(String path, HttpMethod method, Object body, String bearerToken) {
        return exchange(path, method, body, bearerToken, MediaType.APPLICATION_JSON);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, Object body, String bearerToken, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, method, entity, String.class);
    }

    private ResponseEntity<byte[]> exchangeForBytes(String path, HttpMethod method, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, method, entity, byte[].class);
    }

    private ResponseEntity<String> multipartRequest(
            String path,
            HttpMethod method,
            Map<String, Object> parts,
            String bearerToken,
            MediaType fileMediaType) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        parts.forEach((key, value) -> {
            if (value instanceof ByteArrayResource resource) {
                HttpHeaders partHeaders = new HttpHeaders();
                partHeaders.setContentType(fileMediaType);
                body.add(key, new HttpEntity<>(resource, partHeaders));
            } else {
                body.add(key, value);
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }

        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode readJson(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private ByteArrayResource namedResource(String filename, byte[] bytes) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private byte[] pdfBytes(String text) {
        String pdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\nstream\n" + text + "\nendstream\n%%EOF";
        return pdf.getBytes(StandardCharsets.ISO_8859_1);
    }
}
