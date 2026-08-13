package com.sicms.service;

import com.sicms.dto.ForgotPasswordRequest;
import com.sicms.dto.ForgotPasswordResponse;
import com.sicms.dto.ResetPasswordRequest;
import com.sicms.dto.VerifyResetOtpRequest;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.User;
import com.sicms.exception.AuthException;
import com.sicms.repository.RefreshTokenRepository;
import com.sicms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    public PasswordResetService(
            UserRepository userRepository,
            OtpService otpService,
            UserService userService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Forgot password requested for: {}", email);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (Boolean.TRUE.equals(user.getAccountEnabled())) {
                try {
                    otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET);
                    log.info("Forgot password OTP generated and dispatched for: {}", email);
                } catch (Exception ex) {
                    log.error("Failed to generate or send forgot password OTP to {}", email, ex);
                }
            } else {
                log.warn("Forgot password requested for disabled account: {}", email);
            }
        } else {
            log.info("Forgot password requested for non-existent email: {}", email);
        }

        // Return a uniform safe response to prevent user enumeration
        return new ForgotPasswordResponse(
                "success",
                "If an account with that email exists, a password reset OTP has been sent."
        );
    }

    @Transactional
    public boolean verifyResetOtp(VerifyResetOtpRequest request) {
        log.info("Verifying password reset OTP for: {}", request.getEmail());
        User user = otpService.verifyOtpAndGetUser(request.getEmail(), request.getOtp(), OtpPurpose.PASSWORD_RESET);
        return user != null;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for: {}", request.getEmail());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("New password and confirm password do not match");
        }

        validatePasswordPolicy(request.getNewPassword());

        // Verify 6-digit OTP code for PASSWORD_RESET purpose
        User user = otpService.verifyOtpAndGetUser(request.getEmail(), request.getOtp(), OtpPurpose.PASSWORD_RESET);

        // Encode and save new password via central UserService updatePassword method
        userService.updatePassword(user, request.getNewPassword());

        // Revoke all active sessions
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Password successfully reset and sessions revoked for: {}", request.getEmail());
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
