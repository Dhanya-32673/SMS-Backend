package com.sicms.controller;

import com.sicms.dto.*;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
import com.sicms.service.GoogleAuthService;
import com.sicms.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    public AuthController(
            AuthService authService,
            GoogleAuthService googleAuthService,
            OtpService otpService,
            UserRepository userRepository) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginInitiatedResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginInitiatedResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginInitiatedResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginInitiatedResponse response = googleAuthService.authenticateGoogleUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({ "/send-login-otp", "/resend-otp", "/otp/resend", "/otp/send" })
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody OtpSendRequest request) {
        otpService.generateAndSendOtp(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OTP sent successfully to " + request.getEmail()));
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        otpService.generateAndSendOtp(to, com.sicms.entity.OtpPurpose.LOGIN);
        return ResponseEntity.ok("Test email sent through Brevo SMTP to " + to);
    }

    @PostMapping({ "/verify-login-otp", "/verify-otp", "/otp/verify" })
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        LoginResponse response = otpService.verifyLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password reset OTP sent to " + request.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password has been reset successfully. Please login with your new password."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmailIgnoreCase(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return ResponseEntity.ok(new UserDto(user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password changed successfully."));
    }

    @PostMapping("/admin/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminResetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        authService.adminResetUserPassword(userId, request.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "User password reset successfully."));
    }
}
