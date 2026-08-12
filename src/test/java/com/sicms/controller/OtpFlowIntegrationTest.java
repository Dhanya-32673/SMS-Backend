package com.sicms.controller;

import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.entity.Role;
import com.sicms.entity.User;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;
import com.sicms.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OtpFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        if (userRepository.findByEmailIgnoreCase("testotpuser@example.com").isEmpty()) {
            Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator")));

            User testUser = new User();
            testUser.setFullName("Test OTP User");
            testUser.setEmail("testotpuser@example.com");
            testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            testUser.setRole(adminRole);
            testUser.setAccountEnabled(true);
            userRepository.save(testUser);
        }
    }

    @Test
    public void testStrictOtpFlowAndRejection() throws Exception {
        User user = userRepository.findByEmailIgnoreCase("testotpuser@example.com").orElseThrow();

        // 1. Manually insert known OTP code "5678" for testing
        otpRepository.invalidateAllPreviousOtpsForEmail("testotpuser@example.com", OtpPurpose.LOGIN);
        
        OtpVerification verification = new OtpVerification();
        verification.setEmail("testotpuser@example.com");
        verification.setUser(user);
        verification.setPurpose(OtpPurpose.LOGIN);
        verification.setOtpHash(otpService.hashOtp("5678"));
        verification.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        verification.setAttemptCount(0);
        verification.setUsed(false);
        otpRepository.save(verification);

        // 2. Reject hardcoded/wrong OTP "1234"
        mockMvc.perform(post("/api/auth/verify-login-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"testotpuser@example.com\",\"otp\":\"1234\"}"))
                .andExpect(status().isBadRequest());

        // 3. Accept exact matching OTP "5678"
        mockMvc.perform(post("/api/auth/verify-login-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"testotpuser@example.com\",\"otp\":\"5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
