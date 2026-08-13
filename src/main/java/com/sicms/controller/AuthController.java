package com.sicms.controller;

import com.sicms.dto.*;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
import com.sicms.service.EmailService;
import com.sicms.service.GoogleAuthService;
import com.sicms.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

        @PostMapping("/admin/login")
    public ResponseEntity<LoginInitiatedResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginInitiatedResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/verify-otp")
    public ResponseEntity<LoginResponse> verifyAdminOtp(@Valid @RequestBody OtpVerifyRequest request) {
        LoginResponse response = authService.verifyAdminOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/faculty/login")
    public ResponseEntity<LoginResponse> facultyLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.facultyLogin(request);
        return ResponseEntity.ok(response);
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
        System.out.println("=================================================");
        System.out.println(">>> RESEND REQUESTED FOR EMAIL: [" + request.getEmail() + "]");
        System.out.println("=================================================");
        otpService.generateAndSendOtp(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OTP sent successfully to " + request.getEmail()));
    }

    @PostMapping({ "/verify-login-otp", "/verify-otp", "/otp/verify" })
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        LoginResponse response = otpService.verifyLoginOtp(request);
        return ResponseEntity.ok(response);
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
        authService.changePassword(userDetails.getEmail(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
