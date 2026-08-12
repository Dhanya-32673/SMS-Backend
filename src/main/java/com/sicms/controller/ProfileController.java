package com.sicms.controller;

import com.sicms.dto.ChangePasswordRequest;
import com.sicms.dto.UserDto;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.CustomUserDetails;
import com.sicms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public ProfileController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Get current logged-in user profile
     */
    @GetMapping
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmailIgnoreCase(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        return ResponseEntity.ok(new UserDto(user));
    }

    /**
     * Change Password for current logged-in user
     * PUT /api/profile/change-password
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.changePassword(userDetails.getUsername(), request);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password changed successfully."
        ));
    }
}
