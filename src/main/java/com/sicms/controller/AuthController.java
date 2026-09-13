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
    private final com.sicms.repository.StudentRepository studentRepository;

    public AuthController(AuthService authService, UserRepository userRepository, GoogleAuthService googleAuthService, com.sicms.repository.StudentRepository studentRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.googleAuthService = googleAuthService;
        this.studentRepository = studentRepository;
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

    @PostMapping({"/student/login", "/student-login"})
    public ResponseEntity<LoginVerifyResponse> studentLogin(@Valid @RequestBody LoginRequest request) {
        LoginVerifyResponse response = authService.studentLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/faculty/login", "/faculty-login"})
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal Object principal, Principal fallbackPrincipal) {
        String email = null;
        if (principal instanceof CustomUserDetails cud) {
            email = cud.getEmail();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else if (fallbackPrincipal != null) {
            email = fallbackPrincipal.getName();
        }

        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDto dto = new UserDto(user);
        if ((dto.getStudentId() == null || dto.getStudentId().isBlank()) && studentRepository != null) {
            studentRepository.findByEmailOrStudentId(email).ifPresent(s -> {
                dto.setStudentId(s.getStudentId());
            });
        }
        return ResponseEntity.ok(dto);
    }

    @RequestMapping(value = {"/change-password", "/student/change-password"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Object principal,
            Principal fallbackPrincipal) {
        String email = null;
        if (principal instanceof CustomUserDetails cud) {
            email = cud.getEmail();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else if (fallbackPrincipal != null) {
            email = fallbackPrincipal.getName();
        }

        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
