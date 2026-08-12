package com.sicms.controller;

import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.UserRepository;
import com.sicms.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TEMPORARY DEPLOYMENT DEBUGGING ENDPOINTS
 * Mark for removal or secure with ADMIN authority prior to production release.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;

    public DebugController(
            UserRepository userRepository,
            OtpRepository otpRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            long userCount = userRepository.count();
            result.put("status", "SUCCESS");
            result.put("database", "PostgreSQL Connected");
            result.put("totalUsersInDb", userCount);
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> testEmailDispatch(@RequestParam(defaultValue = "bhashyamgnt.edu@gmail.com") String to) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            emailService.sendTestEmail(to);
            result.put("status", "SUCCESS");
            result.put("message", "Test OTP email dispatched successfully to: " + to);
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/otp")
    public ResponseEntity<Map<String, Object>> getLatestOtp(@RequestParam String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Optional<OtpVerification> otp = otpRepository
                    .findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(email.trim().toLowerCase(), OtpPurpose.LOGIN);

            if (otp.isPresent()) {
                result.put("status", "FOUND");
                result.put("email", otp.get().getEmail());
                result.put("purpose", otp.get().getPurpose());
                result.put("expiresAt", otp.get().getExpiresAt());
                result.put("isUsed", otp.get().getUsed());
            } else {
                result.put("status", "NOT_FOUND");
                result.put("message", "No active unused OTP request found for email: " + email);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
