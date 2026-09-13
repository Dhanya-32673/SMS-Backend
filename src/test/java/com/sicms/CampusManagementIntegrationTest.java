package com.sicms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.repository.CampusRepository;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.StudentRepository;
import com.sicms.repository.UserRepository;
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
public class CampusManagementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient())
        );

        Optional<com.sicms.entity.User> adminOpt = userRepository.findByEmailIgnoreCase("admin@college.edu");
        if (adminOpt.isPresent()) {
            com.sicms.entity.User admin = adminOpt.get();
            admin.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
            admin.setAccountEnabled(true);
            admin.setEmailVerified(true);
            userRepository.save(admin);
        }

        JsonNode loginJson = login("admin@college.edu", "AdminPass123!");
        adminToken = loginJson.get("accessToken").asText();
        assertNotNull(adminToken, "Admin access token must be present");
    }

    @Test
    @Order(1)
    void allEighteenCampusesArePresent() throws Exception {
        ResponseEntity<String> response = exchange("/api/campuses", HttpMethod.GET, null, adminToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.isArray());
        assertEquals(18, json.size(), "Should have exactly 18 campuses");

        boolean vaidehiFound = false;
        for (JsonNode node : json) {
            if ("VAIDEHI".equalsIgnoreCase(node.get("name").asText()) && node.get("id").asLong() == 5L) {
                vaidehiFound = true;
                break;
            }
        }
        assertTrue(vaidehiFound, "Campus VAIDEHI with ID 5 must exist");
    }

    @Test
    @Order(2)
    void viewCampusStudentsWorksWithoutSectionError() throws Exception {
        // ID 5 is VAIDEHI
        ResponseEntity<String> response = exchange("/api/campuses/5/students", HttpMethod.GET, null, adminToken);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.isArray(), "Response body should be a list of students");
    }

    @Test
    @Order(3)
    void assignAndUnassignStudentToCampusLifecycle() throws Exception {
        // Find or create test student
        String testStudentId = "STU2026001001";

        // Assign to VAIDEHI (ID 5)
        Map<String, Object> assignPayload = Map.of("studentIds", List.of(testStudentId));
        ResponseEntity<String> assignRes = postJson("/api/campuses/5/students", assignPayload, adminToken);
        assertEquals(HttpStatus.OK, assignRes.getStatusCode());

        // Verify in campus students list
        ResponseEntity<String> studentsRes = exchange("/api/campuses/5/students", HttpMethod.GET, null, adminToken);
        assertEquals(HttpStatus.OK, studentsRes.getStatusCode());
        JsonNode studentsJson = objectMapper.readTree(studentsRes.getBody());
        boolean studentFound = false;
        for (JsonNode s : studentsJson) {
            if (testStudentId.equalsIgnoreCase(s.get("studentId").asText())) {
                studentFound = true;
                assertEquals("VAIDEHI", s.get("campus").asText());
                assertEquals(5L, s.get("campusId").asLong());
                break;
            }
        }
        assertTrue(studentFound, "Student must be assigned to VAIDEHI");

        // Test duplicate assignment prevention
        ResponseEntity<String> dupRes = postJson("/api/campuses/5/students", assignPayload, adminToken);
        assertEquals(HttpStatus.OK, dupRes.getStatusCode());
        JsonNode dupJson = objectMapper.readTree(dupRes.getBody());
        assertEquals(0, dupJson.get("assignedCount").asInt(), "Duplicate assignment must result in 0 new assignments");

        // Unassign student
        ResponseEntity<String> unassignRes = exchange("/api/campuses/5/students/" + testStudentId, HttpMethod.DELETE, null, adminToken);
        assertTrue(unassignRes.getStatusCode().is2xxSuccessful(), "Unassign must succeed with 2xx status");

        // Verify student is removed from VAIDEHI
        ResponseEntity<String> studentsAfter = exchange("/api/campuses/5/students", HttpMethod.GET, null, adminToken);
        JsonNode afterJson = objectMapper.readTree(studentsAfter.getBody());
        for (JsonNode s : afterJson) {
            assertNotEquals(testStudentId, s.get("studentId").asText(), "Student must no longer be in VAIDEHI roster");
        }

        // Verify student record is NOT deleted from database
        assertTrue(studentRepository.findByStudentIdIgnoreCase(testStudentId).isPresent(), "Student entity must remain in database");

        // Re-assign for clean state
        postJson("/api/campuses/5/students", assignPayload, adminToken);
    }

    private JsonNode login(String email, String password) throws Exception {
        ResponseEntity<String> response = postJson("/api/auth/login", Map.of("email", email, "password", password), null);
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());

        String otp = waitForOtp(email, "LOGIN");
        ResponseEntity<String> verifyResponse = postJson("/api/auth/verify-otp", Map.of("email", email, "otp", otp), null);
        assertEquals(HttpStatus.OK, verifyResponse.getStatusCode(), verifyResponse.getBody());

        return objectMapper.readTree(verifyResponse.getBody());
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
}
