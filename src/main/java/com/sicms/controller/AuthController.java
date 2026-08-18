package com.sicms.controller;

import com.sicms.dto.*;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
import com.sicms.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, UserRepository userRepository, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping({"/admin/login", "/login"})
    public ResponseEntity<LoginInitiatedResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginInitiatedResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/admin/verify-otp", "/verify-otp", "/verify-login-otp", "/otp/verify"})
    public ResponseEntity<LoginVerifyResponse> verifyAdminOtp(@Valid @RequestBody OtpVerifyRequest request) {
        LoginVerifyResponse response = authService.verifyAdminOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/admin/send-otp", "/send-login-otp", "/resend-otp", "/otp/send"})
    public ResponseEntity<Map<String, String>> sendAdminOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/faculty/login")
    public ResponseEntity<LoginVerifyResponse> facultyLogin(@Valid @RequestBody LoginRequest request) {
        LoginVerifyResponse response = authService.facultyLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginInitiatedResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginInitiatedResponse response = googleAuthService.authenticateGoogleUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/authorization/google")
    public ResponseEntity<Map<String, String>> oauth2GoogleFallback() {
        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "message", "Google OAuth2 endpoint active. Please send Google ID token to /api/auth/google endpoint."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginVerifyResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LoginVerifyResponse response = authService.refreshAccessToken(request.getRefreshToken());
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmailIgnoreCase(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return ResponseEntity.ok(new UserDto(user));
    }

    @RequestMapping(value = "/change-password", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Principal principal) {
        String email = userDetails != null ? userDetails.getEmail() : (principal != null ? principal.getName() : null);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
