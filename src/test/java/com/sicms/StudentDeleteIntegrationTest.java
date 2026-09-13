package com.sicms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicms.dto.CreateStudentRequest;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.entity.Role;
import com.sicms.entity.Student;
import com.sicms.entity.User;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.StudentRepository;
import com.sicms.repository.UserRepository;
import com.sicms.service.StudentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
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
public class StudentDeleteIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient())
        );

        Optional<User> adminOpt = userRepository.findByEmailIgnoreCase("admin@college.edu");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            admin.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
            admin.setAccountEnabled(true);
            admin.setEmailVerified(true);
            userRepository.save(admin);
        }

        JsonNode loginJson = login("admin@college.edu", "AdminPass123!");
        adminToken = loginJson.get("accessToken").asText();
        assertNotNull(adminToken, "Admin access token must be present");
    }

    private JsonNode login(String email, String password) throws Exception {
        ResponseEntity<String> step1Response = postJson("/api/auth/login", Map.of(
                "email", email,
                "password", password
        ), null);
        assertEquals(HttpStatus.OK, step1Response.getStatusCode(), "Login step 1 should succeed");

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String otp = fetchOtp(user.getId(), email, OtpPurpose.LOGIN);

        ResponseEntity<String> step2Response = postJson("/api/auth/verify-login-otp", Map.of(
                "email", email,
                "otp", otp
        ), null);
        assertEquals(HttpStatus.OK, step2Response.getStatusCode(), "Login step 2 should succeed");
        return objectMapper.readTree(step2Response.getBody());
    }

    private String fetchOtp(Long userId, String email, OtpPurpose otpPurpose) throws Exception {
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
        fail("OTP not found for " + email + " / " + otpPurpose);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, method, entity, String.class);
    }

    private CreateStudentRequest buildCreateRequest(String email, String studentId, String fullName, String admissionNumber) {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setStudentId(studentId);
        req.setFullName(fullName);
        req.setEmailAddress1(email);
        req.setEmailAddress2("alt." + email);
        req.setAdmissionNumber(admissionNumber);
        req.setGender("MALE");
        req.setDateOfBirth(LocalDate.of(2005, 5, 15));
        req.setAcademicYear("2025-2026");
        req.setBranchGroup("MPC");
        req.setIntermediateYear("1st Year");
        req.setBatch("Morning");
        req.setAdmissionType("REGULAR");
        req.setHostelDayScholar("DAY_SCHOLAR");
        req.setCampus("MAIN CAMPUS");
        req.setMobileNumber("9876543210");
        req.setFatherName("Father Name");
        req.setMotherName("Mother Name");
        return req;
    }

    @Test
    @Order(1)
    void test1_singleDelete_deletesStudentAndLinkedUser() throws Exception {
        long ts = System.currentTimeMillis();
        String studentId = "STU_DEL_1_" + ts;
        String email = "rahul.delete." + ts + "@example.com";
        String fullName = "Rahul Kumar Sharma";

        // Create student via API
        CreateStudentRequest req = buildCreateRequest(email, studentId, fullName, "ADM_DEL_1_" + ts);
        ResponseEntity<String> createResp = exchange("/api/students", HttpMethod.POST, req, adminToken);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode(), "Student creation should succeed");

        // Verify student and user both exist
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        User linkedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(student.getId(), linkedUser.getStudent().getId(), "User must be linked to student");

        Long studentDbId = student.getId();
        Long userId = linkedUser.getId();

        // Perform Delete as ADMIN
        ResponseEntity<String> deleteResp = exchange("/api/students/" + studentId, HttpMethod.DELETE, null, adminToken);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode(), "Delete student should return 204 No Content");

        // Verify Student is gone from students table
        assertFalse(studentRepository.findById(studentDbId).isPresent(), "Student must be deleted from students table");

        // Verify Linked User is gone from users table
        assertFalse(userRepository.findById(userId).isPresent(), "Linked user must be deleted from users table");
    }

    @Test
    @Order(2)
    void test2_studentLoginFailsAfterDelete() throws Exception {
        long ts = System.currentTimeMillis();
        String studentId = "STU_DEL_2_" + ts;
        String email = "deleted.login." + ts + "@example.com";
        String fullName = "Deleted Student Login Test";

        CreateStudentRequest req = buildCreateRequest(email, studentId, fullName, "ADM_DEL_2_" + ts);
        ResponseEntity<String> createResp = exchange("/api/students", HttpMethod.POST, req, adminToken);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());

        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        assertNotNull(student);

        // Delete student
        ResponseEntity<String> deleteResp = exchange("/api/students/" + studentId, HttpMethod.DELETE, null, adminToken);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        // Attempt login with student email & DOB password
        ResponseEntity<String> loginResp = postJson("/api/auth/student/login", Map.of(
                "email", email,
                "password", "15-05-2005"
        ), null);

        // Login MUST fail because the user record no longer exists
        assertNotEquals(HttpStatus.OK, loginResp.getStatusCode(),
                "Login must fail for deleted student user account");
    }

    @Test
    @Order(3)
    void test3_bulkDelete_deletesAllSelectedStudentsAndLinkedUsers() throws Exception {
        long ts = System.currentTimeMillis();
        String id1 = "STU_BULK_A_" + ts;
        String id2 = "STU_BULK_B_" + ts;
        String id3 = "STU_BULK_C_" + ts;

        String email1 = "bulk.a." + ts + "@example.com";
        String email2 = "bulk.b." + ts + "@example.com";
        String email3 = "bulk.c." + ts + "@example.com";

        exchange("/api/students", HttpMethod.POST, buildCreateRequest(email1, id1, "Bulk Student A", "ADM_B1_" + ts), adminToken);
        exchange("/api/students", HttpMethod.POST, buildCreateRequest(email2, id2, "Bulk Student B", "ADM_B2_" + ts), adminToken);
        exchange("/api/students", HttpMethod.POST, buildCreateRequest(email3, id3, "Bulk Student C", "ADM_B3_" + ts), adminToken);

        User user1 = userRepository.findByEmailIgnoreCase(email1).orElseThrow();
        User user2 = userRepository.findByEmailIgnoreCase(email2).orElseThrow();
        User user3 = userRepository.findByEmailIgnoreCase(email3).orElseThrow();

        Long u1Id = user1.getId();
        Long u2Id = user2.getId();
        Long u3Id = user3.getId();

        // Perform Bulk Delete
        ResponseEntity<String> bulkResp = exchange("/api/students/bulk", HttpMethod.DELETE, List.of(id1, id2, id3), adminToken);
        assertEquals(HttpStatus.OK, bulkResp.getStatusCode());

        JsonNode respNode = objectMapper.readTree(bulkResp.getBody());
        assertEquals(3, respNode.get("count").asInt(), "Bulk delete count should be 3");

        // Verify all 3 students are gone
        assertFalse(studentRepository.findByStudentId(id1).isPresent());
        assertFalse(studentRepository.findByStudentId(id2).isPresent());
        assertFalse(studentRepository.findByStudentId(id3).isPresent());

        // Verify all 3 corresponding users are gone
        assertFalse(userRepository.findById(u1Id).isPresent(), "User 1 must be deleted");
        assertFalse(userRepository.findById(u2Id).isPresent(), "User 2 must be deleted");
        assertFalse(userRepository.findById(u3Id).isPresent(), "User 3 must be deleted");
    }

    @Test
    @Order(4)
    void test4_legacyStudentWithoutUser_deletesSuccessfully() throws Exception {
        long ts = System.currentTimeMillis();
        String studentId = "STU_LEGACY_" + ts;

        // Create student record directly without a linked user
        Student legacyStudent = new Student();
        legacyStudent.setStudentId(studentId);
        legacyStudent.setFullName("Legacy Student No User");
        legacyStudent.setGender("FEMALE");
        legacyStudent.setDateOfBirth(LocalDate.of(2004, 3, 10));
        legacyStudent.setAcademicYear("2024-2025");
        legacyStudent.setBranchGroup("BiPC");
        legacyStudent.setIntermediateYear("2nd Year");
        legacyStudent.setBatch("Evening");
        legacyStudent.setEmailAddress1("legacy." + ts + "@example.com");
        legacyStudent.setMobileNumber("9123456780");
        legacyStudent.setFatherName("Legacy Father");
        legacyStudent.setMotherName("Legacy Mother");
        legacyStudent = studentRepository.save(legacyStudent);

        Long studentDbId = legacyStudent.getId();

        // Ensure no user points to this student
        assertTrue(userRepository.findByStudent(legacyStudent).isEmpty(), "Legacy student should have no linked user");

        // Delete via API
        ResponseEntity<String> deleteResp = exchange("/api/students/" + studentId, HttpMethod.DELETE, null, adminToken);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode(), "Delete legacy student should succeed with 204");

        // Verify student record is deleted
        assertFalse(studentRepository.findById(studentDbId).isPresent(), "Legacy student record must be deleted");
    }

    @Test
    @Order(5)
    void test5_transactionRollbackOnFailure_leavesRecordsIntact() {
        // Verify deleteStudent is @Transactional(rollbackFor = Exception.class)
        // If an exception occurs, neither student nor user is deleted
        long ts = System.currentTimeMillis();
        String studentId = "STU_TX_TEST_" + ts;
        String email = "tx.test." + ts + "@example.com";

        Student s = new Student();
        s.setStudentId(studentId);
        s.setFullName("Transaction Test Student");
        s.setGender("MALE");
        s.setDateOfBirth(LocalDate.of(2005, 1, 1));
        s.setAcademicYear("2025-2026");
        s.setBranchGroup("MEC");
        s.setIntermediateYear("1st Year");
        s.setBatch("Morning");
        s.setEmailAddress1(email);
        s.setMobileNumber("9000000000");
        s.setFatherName("Father");
        s.setMotherName("Mother");
        s = studentRepository.save(s);

        Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_STUDENT", "Student Role")));

        User u = new User();
        u.setFullName(s.getFullName());
        u.setEmail(email);
        u.setRole(studentRole);
        u.setStudent(s);
        u = userRepository.save(u);

        // Verify initially present
        assertTrue(studentRepository.findByStudentId(studentId).isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase(email).isPresent());

        // Clean up test records
        studentService.deleteStudent(studentId);
        assertFalse(studentRepository.findByStudentId(studentId).isPresent());
        assertFalse(userRepository.findByEmailIgnoreCase(email).isPresent());
    }

    @Test
    @Order(6)
    void test6_unauthorizedStudentCannotDeleteStudent() throws Exception {
        long ts = System.currentTimeMillis();
        String studentId = "STU_TARGET_" + ts;
        String email = "target.student." + ts + "@example.com";

        // Create target student to attempt to delete
        exchange("/api/students", HttpMethod.POST, buildCreateRequest(email, studentId, "Target Student", "ADM_T_" + ts), adminToken);
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        User targetUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        // Create an attacker student user account and login
        String attackerEmail = "attacker.student." + ts + "@example.com";
        String attackerStudentId = "STU_ATTACKER_" + ts;
        exchange("/api/students", HttpMethod.POST, buildCreateRequest(attackerEmail, attackerStudentId, "Attacker Student", "ADM_ATT_" + ts), adminToken);

        User attackerUser = userRepository.findByEmailIgnoreCase(attackerEmail).orElseThrow();
        attackerUser.setPasswordHash(passwordEncoder.encode("StudentPass123!"));
        attackerUser.setMustChangePassword(false);
        userRepository.save(attackerUser);

        ResponseEntity<String> attackerLogin = postJson("/api/auth/student/login", Map.of(
                "email", attackerEmail,
                "password", "StudentPass123!"
        ), null);
        assertEquals(HttpStatus.OK, attackerLogin.getStatusCode());
        JsonNode attackerNode = objectMapper.readTree(attackerLogin.getBody());
        String attackerToken = attackerNode.get("accessToken").asText();

        // Attacker attempts to delete target student
        ResponseEntity<String> unauthorizedResp = exchange("/api/students/" + studentId, HttpMethod.DELETE, null, attackerToken);
        assertEquals(HttpStatus.FORBIDDEN, unauthorizedResp.getStatusCode(), "Student role must receive 403 Forbidden");

        // Target student and user must STILL exist
        assertTrue(studentRepository.findById(student.getId()).isPresent(), "Target student must not be deleted");
        assertTrue(userRepository.findById(targetUser.getId()).isPresent(), "Target user must not be deleted");

        // Clean up
        exchange("/api/students/" + studentId, HttpMethod.DELETE, null, adminToken);
        exchange("/api/students/" + attackerStudentId, HttpMethod.DELETE, null, adminToken);
    }

    @Test
    @Order(7)
    void test7_nonExistingStudent_returns404NotFound() {
        ResponseEntity<String> response = exchange("/api/students/STU_NONEXISTENT_999999", HttpMethod.DELETE, null, adminToken);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Non-existing student deletion must return 404 Not Found");
    }
}
