package com.sicms.controller;

import com.sicms.dto.ForgotPasswordRequest;
import com.sicms.dto.ForgotPasswordResponse;
import com.sicms.dto.ResetPasswordRequest;
import com.sicms.dto.VerifyResetOtpRequest;
import com.sicms.service.AuthService;
import com.sicms.service.EmailService;
import com.sicms.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth", "/api"})
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final AuthService authService;
    private final EmailService emailService;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            AuthService authService,
            EmailService emailService) {
        this.passwordResetService = passwordResetService;
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        passwordResetService.verifyResetOtp(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OTP code verified successfully."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password has been reset successfully. Please login with your new password."
        ));
    }

    @PostMapping({"/admin/users/{userId}/reset-password", "/auth/admin/users/{userId}/reset-password"})
    public ResponseEntity<Map<String, String>> adminResetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        String newPassword = request != null ? request.get("newPassword") : null;
        authService.adminResetUserPassword(userId, newPassword);
        return ResponseEntity.ok(Map.of("message", "User password reset successfully by admin"));
    }

    @GetMapping({"/test/mail", "/test-mail"})
    public ResponseEntity<String> testMail(@RequestParam String email) {
        emailService.sendOtpEmail(email, "123456", "PASSWORD_RESET");
        return ResponseEntity.ok("Mail sent to " + email);
    }
}
