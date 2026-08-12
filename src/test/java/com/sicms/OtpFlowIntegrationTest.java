package com.sicms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.entity.Role;
import com.sicms.entity.User;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OtpFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail = "testuser_otp@college.edu";

    @BeforeEach
    void setUp() {
        otpRepository.deleteAll();
        
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(testEmail);
        if (existingUser.isEmpty()) {
            Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Administrator Role")));

            User user = new User();
            user.setEmail(testEmail);
            user.setFullName("Integration Test User");
            user.setPasswordHash(passwordEncoder.encode("TestPass123!"));
            user.setRole(adminRole);
            user.setAccountEnabled(true);
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    @Test
    void testCompleteOtpFlow() throws Exception {
        // Step 1: Call send-login-otp
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> loginBody = Map.of(
                "email", testEmail,
                "password", "TestPass123!"
        );

        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(loginBody, headers);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
        assertTrue(loginJson.get("requiresOtp").asBoolean());

        // Step 2: Verify OTP exists in Database
        User user = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        Optional<OtpVerification> otpOpt = otpRepository.findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(user, OtpPurpose.LOGIN);
        assertTrue(otpOpt.isPresent(), "OTP should be created in DB");

        String hashedOtp = otpOpt.get().getOtpHash();
        assertNotNull(hashedOtp, "Hashed OTP must not be null");

        // Crack raw OTP for testing (1000..9999)
        String rawOtp = null;
        for (int i = 1000; i <= 9999; i++) {
            String candidate = String.format("%04d", i);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(candidate.getBytes(StandardCharsets.UTF_8));
            String candidateHash = Base64.getEncoder().encodeToString(hash);
            if (candidateHash.equals(hashedOtp)) {
                rawOtp = candidate;
                break;
            }
        }
        assertNotNull(rawOtp, "Raw 4-digit OTP should be resolvable from hash");

        // Step 3: Call verify-login-otp
        Map<String, String> verifyBody = Map.of(
                "email", testEmail,
                "otpCode", rawOtp
        );

        HttpEntity<Map<String, String>> verifyRequest = new HttpEntity<>(verifyBody, headers);
        ResponseEntity<String> verifyResponse = restTemplate.postForEntity("/api/auth/verify-login-otp", verifyRequest, String.class);

        assertEquals(HttpStatus.OK, verifyResponse.getStatusCode());
        JsonNode verifyJson = objectMapper.readTree(verifyResponse.getBody());
        
        assertNotNull(verifyJson.get("accessToken"), "Access token must be returned");
        assertNotNull(verifyJson.get("refreshToken"), "Refresh token must be returned");
        assertEquals(testEmail, verifyJson.get("user").get("email").asText());

        // Assert OTP marked as used
        Optional<OtpVerification> updatedOtp = otpRepository.findById(otpOpt.get().getId());
        assertTrue(updatedOtp.isPresent() && Boolean.TRUE.equals(updatedOtp.get().getUsed()), "OTP must be marked as used");
    }
}
