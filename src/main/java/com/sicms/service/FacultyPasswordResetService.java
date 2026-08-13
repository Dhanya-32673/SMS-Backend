package com.sicms.service;

import com.sicms.dto.AdminFacultyResetPasswordRequest;
import com.sicms.dto.FacultyForgotPasswordRequest;
import com.sicms.dto.ForgotPasswordResponse;
import com.sicms.entity.Faculty;
import com.sicms.entity.FacultyPasswordResetRequest;
import com.sicms.entity.User;
import com.sicms.exception.AuthException;
import com.sicms.repository.FacultyPasswordResetRequestRepository;
import com.sicms.repository.FacultyRepository;
import com.sicms.repository.RefreshTokenRepository;
import com.sicms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class FacultyPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(FacultyPasswordResetService.class);

    private final FacultyPasswordResetRequestRepository resetRequestRepository;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;

    public FacultyPasswordResetService(
            FacultyPasswordResetRequestRepository resetRequestRepository,
            UserRepository userRepository,
            FacultyRepository facultyRepository,
            UserService userService,
            EmailService emailService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.resetRequestRepository = resetRequestRepository;
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public ForgotPasswordResponse requestFacultyPasswordReset(FacultyForgotPasswordRequest request) {
        if (request == null || request.getFacultyEmail() == null || request.getFacultyEmail().isBlank()) {
            throw new AuthException("Faculty email is required.");
        }

        String email = request.getFacultyEmail().trim().toLowerCase();
        log.info("Faculty forgot password request received for: {}", email);

        // Rate-limiting check: max 3 requests per hour
        long recentCount = resetRequestRepository.countByFacultyEmailIgnoreCaseAndRequestedAtAfter(
                email,
                LocalDateTime.now().minusHours(1)
        );
        if (recentCount >= 3) {
            log.warn("Rate limit exceeded for faculty password reset: {}", email);
            throw new AuthException("Too many password reset requests. Please wait an hour before requesting again.");
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getAccountEnabled())) {
                try {
                    // Invalidate previous active requests for this email
                    resetRequestRepository.invalidateAllPreviousRequestsForEmail(email);

                    // Generate secure 6-digit OTP
                    SecureRandom random = new SecureRandom();
                    String rawOtp = String.format("%06d", random.nextInt(1000000));
                    String otpHash = hashOtp(rawOtp);
                    LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

                    FacultyPasswordResetRequest resetReq = new FacultyPasswordResetRequest(
                            email,
                            request.getEmployeeId(),
                            request.getReason(),
                            otpHash,
                            expiry
                    );
                    resetRequestRepository.save(resetReq);

                    Optional<Faculty> facultyOpt = facultyRepository.findByEmail(email);
                    String facultyName = facultyOpt.map(Faculty::getFullName).orElse(user.getFullName());
                    String empId = request.getEmployeeId() != null && !request.getEmployeeId().isBlank()
                            ? request.getEmployeeId()
                            : facultyOpt.map(Faculty::getEmployeeId).orElse(null);

                    // Dispatch OTP to ADMIN EMAIL ONLY
                    emailService.sendFacultyResetOtpToAdmin(
                            facultyName,
                            email,
                            empId,
                            rawOtp,
                            request.getReason(),
                            resetReq.getRequestedAt(),
                            resetReq.getOtpExpiry()
                    );

                    log.info("Faculty reset OTP dispatched exclusively to Admin for faculty email={}", email);
                } catch (Exception e) {
                    log.error("Failed to process faculty password reset request for {}", email, e);
                }
            } else {
                log.warn("Faculty password reset requested for disabled user account: {}", email);
            }
        } else {
            log.info("Faculty password reset requested for non-existent email: {}", email);
        }

        // Always return generic safe message to prevent user enumeration
        return new ForgotPasswordResponse(
                "success",
                "Your password reset request has been sent to the administrator."
        );
    }

    @Transactional
    public Map<String, String> adminResetFacultyPassword(AdminFacultyResetPasswordRequest request, String adminEmail) {
        if (request == null) {
            throw new AuthException("Invalid request data.");
        }

        String email = request.getFacultyEmail().trim().toLowerCase();
        log.info("Admin [{}] attempting faculty password reset for email={}", adminEmail, email);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("New password and confirm password do not match.");
        }

        validatePasswordPolicy(request.getNewPassword());

        FacultyPasswordResetRequest resetReq = resetRequestRepository
                .findFirstByFacultyEmailIgnoreCaseAndUsedFalseOrderByRequestedAtDesc(email)
                .orElseThrow(() -> new AuthException("No active pending password reset request found for faculty email: " + email));

        if (resetReq.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthException("Faculty password reset OTP has expired. Please ask the faculty to submit a new request.");
        }

        String candidateHash = hashOtp(request.getOtp().trim());
        if (!candidateHash.equalsIgnoreCase(resetReq.getOtpHash())) {
            throw new AuthException("Invalid OTP code.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Faculty user account not found for email: " + email));

        // Update password using central UserService logic
        userService.updatePassword(user, request.getNewPassword());

        // Mark request as used
        resetReq.setUsed(true);
        resetReq.setApprovedAt(LocalDateTime.now());
        resetReq.setApprovedByAdmin(adminEmail);
        resetRequestRepository.save(resetReq);

        // Revoke active sessions
        refreshTokenRepository.revokeAllUserTokens(user);

        // Send confirmation email to faculty
        emailService.sendFacultyPasswordResetConfirmation(email);

        log.info("AUDIT LOG: Admin [{}] successfully reset password for faculty [{}]", adminEmail, email);

        return Map.of(
                "status", "success",
                "message", "Faculty password has been reset successfully and confirmation email sent to faculty."
        );
    }

    private String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not available", e);
        }
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new AuthException("Password does not meet security requirements. Must be at least 8 characters.");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new AuthException("Password does not meet security requirements. Must contain uppercase, lowercase, digit, and special character.");
        }
    }
}
