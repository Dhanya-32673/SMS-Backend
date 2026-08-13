package com.sicms.controller;

import com.sicms.dto.*;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
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

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<LoginInitiatedResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginInitiatedResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/verify-otp")
    public ResponseEntity<AuthResponse> verifyAdminOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyAdminOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/faculty/login")
    public ResponseEntity<AuthResponse> facultyLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.facultyLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginInitiatedResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginInitiatedResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
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
