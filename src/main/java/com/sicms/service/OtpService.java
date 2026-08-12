package com.sicms.service;

import com.sicms.dto.LoginResponse;
import com.sicms.dto.OtpSendRequest;
import com.sicms.dto.OtpVerifyRequest;
import com.sicms.dto.UserDto;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.entity.User;
import com.sicms.exception.AccountDisabledException;
import com.sicms.exception.AuthException;
import com.sicms.repository.OtpRepository;
import com.sicms.repository.UserRepository;
import com.sicms.security.JwtService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            UserRepository userRepository,
            OtpRepository otpRepository,
            EmailService emailService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        String cleanEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new AuthException("No registered account found with email: " + cleanEmail));

        if (Boolean.FALSE.equals(user.getAccountEnabled())) {
            throw new AccountDisabledException("Your account has been disabled. Please contact the administrator.");
        }

        // Max 50 Requests Per Hour Rate Limit Check
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long countInLastHour = otpRepository.countByEmailIgnoreCaseAndCreatedAtAfter(cleanEmail, oneHourAgo);
        if (countInLastHour >= 50) {
            throw new AuthException("Maximum OTP requests (50 per hour) exceeded. Please try again later.");
        }

        // 10-Second Resend Cooldown Check
        Optional<OtpVerification> recentOtp = otpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(cleanEmail, purpose);

        if (recentOtp.isPresent()) {
            Instant createdAt = recentOtp.get().getCreatedAt();
            if (createdAt != null) {
                long secondsElapsed = java.time.Duration.between(createdAt, Instant.now()).getSeconds();
                if (secondsElapsed >= 0 && secondsElapsed < 10) {
                    long remainingSeconds = 10 - secondsElapsed;
                    throw new AuthException("Please wait " + remainingSeconds + " seconds before requesting a new OTP.");
                }
            }
        }

        // Invalidate previous active OTPs for this email & purpose
        otpRepository.invalidateAllPreviousOtpsForEmail(cleanEmail, purpose);

        // Generate random 4-digit OTP (1000–9999)
        String rawOtp = String.format("%04d", 1000 + secureRandom.nextInt(9000));
        String hashedOtp = hashOtp(rawOtp);

        OtpVerification verification = new OtpVerification();
        verification.setEmail(cleanEmail);
        verification.setUser(user);
        verification.setPurpose(purpose);
        verification.setOtpHash(hashedOtp);
        verification.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES)); // 5 Minutes Validity
        verification.setAttemptCount(0);
        verification.setUsed(false);

        otpRepository.save(verification);

        // Dispatch OTP via Email
        emailService.sendOtpEmail(user.getEmail(), rawOtp, purpose.name());
    }

    @Transactional
    public void generateAndSendOtp(OtpSendRequest request) {
        OtpPurpose purpose = parsePurpose(request.getPurpose());
        generateAndSendOtp(request.getEmail(), purpose);
    }

    @Transactional
    public LoginResponse verifyLoginOtp(OtpVerifyRequest request) {
        User user = verifyOtpAndGetUser(request.getEmail(), request.getOtp(), OtpPurpose.LOGIN);

        user.setLastLogin(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new LoginResponse(
                accessToken,
                jwtService.getAccessExpirationSeconds(),
                refreshToken,
                new UserDto(savedUser)
        );
    }

    @Transactional
    public User verifyOtpAndGetUser(OtpVerifyRequest request, OtpPurpose expectedPurpose) {
        if (request == null) {
            throw new AuthException("OTP verification request cannot be null.");
        }
        return verifyOtpAndGetUser(request.getEmail(), request.getOtp(), expectedPurpose);
    }

    @Transactional
    public User verifyOtpAndGetUser(String rawEmail, String rawOtp, OtpPurpose expectedPurpose) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new AuthException("Email is required for OTP verification.");
        }
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new AuthException("OTP code is required.");
        }

        String email = rawEmail.trim().toLowerCase();
        String cleanOtp = rawOtp.trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("User not found for email: " + email));

        OtpVerification verification = otpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, expectedPurpose)
                .orElseThrow(() -> new AuthException("No active OTP request found or OTP already used."));

        // Expiration check (5 minutes)
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            verification.setUsed(true);
            otpRepository.save(verification);
            throw new AuthException("OTP code has expired. Please request a new OTP.");
        }

        // Maximum attempts check (Max 5 attempts)
        int currentAttempts = verification.getAttemptCount() + 1;
        verification.setAttemptCount(currentAttempts);

        if (currentAttempts > 5) {
            verification.setUsed(true);
            otpRepository.save(verification);
            throw new AuthException("Maximum verification attempts (5) exceeded. Please request a new OTP.");
        }

        // Verify SHA-256 Hash strictly against database
        String inputHash = hashOtp(cleanOtp);
        boolean isValid = verification.getOtpHash().equals(inputHash);
        if (!isValid) {
            otpRepository.save(verification);
            int remaining = 5 - currentAttempts;
            throw new AuthException("Invalid OTP code. " + (remaining > 0 ? remaining + " attempt(s) remaining." : "Please request a new code."));
        }

        // Verification successful: Mark as used
        verification.setUsed(true);
        otpRepository.save(verification);

        return user;
    }

    private OtpPurpose parsePurpose(String purposeStr) {
        if (purposeStr == null || purposeStr.isBlank()) {
            return OtpPurpose.LOGIN;
        }
        try {
            return OtpPurpose.valueOf(purposeStr.toUpperCase());
        } catch (Exception e) {
            return OtpPurpose.LOGIN;
        }
    }

    public String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing OTP code", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            otpRepository.deleteByExpiresAtBefore(Instant.now());
        } catch (Exception e) {
            System.err.println("Warning: Unable to clean up expired OTP records: " + e.getMessage());
        }
    }
}
