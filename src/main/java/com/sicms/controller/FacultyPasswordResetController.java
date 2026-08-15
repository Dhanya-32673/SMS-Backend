package com.sicms.controller;

import com.sicms.dto.AdminFacultyResetPasswordRequest;
import com.sicms.dto.FacultyForgotPasswordRequest;
import com.sicms.dto.ForgotPasswordResponse;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.FacultyPasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping
public class FacultyPasswordResetController {

    private final FacultyPasswordResetService facultyPasswordResetService;

    public FacultyPasswordResetController(FacultyPasswordResetService facultyPasswordResetService) {
        this.facultyPasswordResetService = facultyPasswordResetService;
    }

    /**
     * Step 1: Faculty submits password reset request.
     * Public endpoint. Dispatches OTP to Admin Email ONLY.
     */
    @PostMapping({"/api/auth/faculty/forgot-password", "/auth/faculty/forgot-password"})
    public ResponseEntity<ForgotPasswordResponse> requestFacultyReset(
            @Valid @RequestBody FacultyForgotPasswordRequest request
    ) {
        ForgotPasswordResponse response = facultyPasswordResetService.requestFacultyPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: Admin enters OTP received in email and updates faculty password.
     * Requires ROLE_ADMIN authority.
     */
    @PostMapping({"/api/admin/faculty/reset-password", "/admin/faculty/reset-password"})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> adminResetFacultyPassword(
            @Valid @RequestBody AdminFacultyResetPasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Principal principal
    ) {
        String adminEmail = userDetails != null ? userDetails.getEmail() : (principal != null ? principal.getName() : "bhashyamgnt.edu@gmail.com");
        Map<String, String> response = facultyPasswordResetService.adminResetFacultyPassword(request, adminEmail);
        return ResponseEntity.ok(response);
    }
}
