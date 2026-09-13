package com.sicms.service;

import org.springframework.security.access.AccessDeniedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sicms.dto.*;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.User;
import com.sicms.exception.AccountDisabledException;
import com.sicms.exception.AuthException;
import com.sicms.exception.InvalidCredentialsException;
import com.sicms.repository.RefreshTokenRepository;
import com.sicms.repository.UserRepository;
import com.sicms.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final com.sicms.repository.RoleRepository roleRepository;
    private final com.sicms.repository.StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    public AuthService(
            UserRepository userRepository,
            com.sicms.repository.RoleRepository roleRepository,
            com.sicms.repository.StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            OtpService otpService,
            RefreshTokenRepository refreshTokenRepository,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    @Transactional
    public LoginInitiatedResponse adminLogin(LoginRequest request) {
        User user = validateEmailPasswordLogin(request);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        if (!"ROLE_ADMIN".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("Role mismatch: User is not an ADMIN");
        }

        otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.LOGIN);
        return new LoginInitiatedResponse("OTP sent successfully to admin email", user.getEmail(), true);
    }

    @Transactional
    public LoginVerifyResponse verifyAdminOtp(OtpVerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = otpService.verifyOtpAndGetUser(email, request.getOtp(), OtpPurpose.LOGIN);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        if (!"ROLE_ADMIN".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("Role mismatch: User is not an ADMIN");
        }

        return issueTokensForUser(user);
    }

    @Transactional
    public LoginVerifyResponse studentLogin(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException("Email and password are required");
        }

        String emailOrId = request.getEmail().trim().toLowerCase();

        // 1. Check existing User record by email
        java.util.Optional<User> userOpt = userRepository.findByEmailIgnoreCase(emailOrId);

        // 2. If user not found directly in users table, lookup student in students table
        if (userOpt.isEmpty()) {
            java.util.Optional<com.sicms.entity.Student> studentOpt = studentRepository.findByEmailOrStudentId(emailOrId);
            if (studentOpt.isPresent()) {
                com.sicms.entity.Student student = studentOpt.get();
                String targetEmail = (student.getEmailAddress1() != null && !student.getEmailAddress1().isBlank())
                        ? student.getEmailAddress1().trim().toLowerCase()
                        : (student.getStudentId() != null ? student.getStudentId().toLowerCase() + "@student.bhashyam.edu" : emailOrId);

                com.sicms.entity.Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                        .orElseGet(() -> roleRepository.findByRoleName("STUDENT")
                        .orElseGet(() -> roleRepository.save(new com.sicms.entity.Role("ROLE_STUDENT", "Student User Role"))));

                userOpt = userRepository.findByEmailIgnoreCase(targetEmail);
                if (userOpt.isEmpty()) {
                    String defaultPassword = StudentService.formatDobPassword(student.getDateOfBirth());
                    if (defaultPassword == null || defaultPassword.isBlank()) {
                        defaultPassword = "01-01-2000";
                    }

                    User newStudentUser = new User();
                    newStudentUser.setFullName(student.getFullName());
                    newStudentUser.setEmail(targetEmail);
                    newStudentUser.setPasswordHash(passwordEncoder.encode(defaultPassword));
                    newStudentUser.setRole(studentRole);
                    newStudentUser.setAuthProvider(com.sicms.entity.AuthProvider.LOCAL);
                    newStudentUser.setEmailVerified(true);
                    newStudentUser.setAccountEnabled(true);
                    newStudentUser.setMustChangePassword(true);
                    newStudentUser.setStudent(student);

                    userOpt = java.util.Optional.of(userRepository.save(newStudentUser));
                }
            }
        }

        User user = userOpt.orElseThrow(() -> new InvalidCredentialsException("Invalid credentials or student account not found"));

        if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
            throw new AccountDisabledException("Account is disabled");
        }

        // Ensure student relationship is linked if available
        com.sicms.entity.Student student = user.getStudent();
        if (student == null) {
            String targetId = user.getEmail() != null ? user.getEmail() : emailOrId;
            student = studentRepository.findByEmailOrStudentId(targetId).orElse(null);
            if (student == null && emailOrId != null) {
                student = studentRepository.findByEmailOrStudentId(emailOrId).orElse(null);
            }
            if (student == null && targetId != null && targetId.contains("@")) {
                String prefix = targetId.substring(0, targetId.indexOf("@")).trim();
                student = studentRepository.findByStudentIdIgnoreCase(prefix).orElse(null);
            }
            if (student != null) {
                user.setStudent(student);
                userRepository.save(user);
            }
        }

        // Validate strictly against the encoded password hash in the database
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!matches) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        if (!"ROLE_STUDENT".equalsIgnoreCase(roleName) && !"STUDENT".equalsIgnoreCase(roleName)) {
            // If user exists with another role (e.g. legacy role), ensure role is ROLE_STUDENT if student profile exists
            boolean isStudentProfile = studentRepository.findByEmailOrStudentId(user.getEmail()).isPresent();
            if (isStudentProfile) {
                com.sicms.entity.Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                        .orElseGet(() -> roleRepository.findByRoleName("STUDENT")
                        .orElseGet(() -> roleRepository.save(new com.sicms.entity.Role("ROLE_STUDENT", "Student User Role"))));
                user.setRole(studentRole);
                userRepository.save(user);
            } else {
                throw new AccessDeniedException("Role mismatch: User is not a STUDENT");
            }
        }

        return issueTokensForUser(user);
    }

    @Transactional
    public LoginVerifyResponse facultyLogin(LoginRequest request) {
        User user = validateEmailPasswordLogin(request);
        return issueTokensForUser(user);
    }

    @Transactional
    public LoginInitiatedResponse login(LoginRequest request) {
        User user = validateEmailPasswordLogin(request);
        otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.LOGIN);
        return new LoginInitiatedResponse("OTP sent successfully", user.getEmail(), true);
    }

    @Transactional
    public LoginVerifyResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = otpService.verifyOtpAndGetUser(email, request.getOtp(), OtpPurpose.LOGIN);
        return issueTokensForUser(user);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user details"));

        if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
            throw new AccountDisabledException("Account is disabled");
        }

        OtpPurpose purpose = OtpPurpose.LOGIN;
        if (request.getPurpose() != null && !request.getPurpose().isBlank()) {
            try {
                purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());
            } catch (IllegalArgumentException e) {
                // fallback to LOGIN
            }
        }

        otpService.generateAndSendOtp(email, purpose);
    }

    @Transactional
    public LoginVerifyResponse refreshAccessToken(String refreshTokenStr) {
        User user = refreshTokenService.verifyAndRotateRefreshToken(refreshTokenStr);
        return issueTokensForUser(user);
    }

    private User validateEmailPasswordLogin(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
            throw new AccountDisabledException("Account is disabled");
        }

        String rawPass = request.getPassword();
        String storedHash = user.getPasswordHash();

        boolean matches = passwordEncoder.matches(rawPass, storedHash);
        if (!matches) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return user;
    }

    private LoginVerifyResponse issueTokensForUser(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenStr = refreshTokenService.createRefreshToken(user);
        UserDto userDto = new UserDto(user);

        return new LoginVerifyResponse(
                true,
                true,
                accessToken,
                86400000L,
                refreshTokenStr,
                null,
                "Authentication successful",
                userDto
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new AuthException("User authentication required");
        }
        if (request == null) {
            throw new AuthException("Invalid change password request");
        }

        String confirm = request.getConfirmNewPassword() != null ? request.getConfirmNewPassword() : request.getConfirmPassword();
        if (!request.getNewPassword().equals(confirm)) {
            throw new AuthException("New password and confirm password do not match.");
        }

        validatePasswordPolicy(request.getNewPassword());

        User user = userRepository.findByEmailIgnoreCase(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new AuthException("User account not found"));

        log.info("Change password request for {}", userEmail);
        log.info("Password hash exists: {}", user.getPasswordHash() != null);
        boolean matches = passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash());
        log.info("Password match result: {}", matches);

        if (!matches) {
            throw new AuthException("Current password is incorrect.");
        }

        if (user.getStudent() != null && user.getStudent().getDateOfBirth() != null) {
            String dobPass = StudentService.formatDobPassword(user.getStudent().getDateOfBirth());
            if (request.getNewPassword().trim().equals(dobPass)) {
                throw new AuthException("Please choose a new password instead of your initial password.");
            }
        }

        userService.updatePassword(user, request.getNewPassword());
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Password successfully updated and tokens revoked for user={}", userEmail);
    }

    @Transactional
    public void adminResetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User account not found with ID: " + userId));

        validatePasswordPolicy(newPassword);

        userService.updatePassword(user, newPassword);
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    public void validatePasswordPolicy(String password) {
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
