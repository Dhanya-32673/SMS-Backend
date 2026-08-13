package com.sicms.controller;

import com.sicms.dto.ChangePasswordRequest;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Change Password for current logged-in user
     * PUT /api/users/change-password
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Processing password change request via /api/users endpoint for user={}", userDetails.getEmail());
        authService.changePassword(userDetails.getUsername(), request);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
