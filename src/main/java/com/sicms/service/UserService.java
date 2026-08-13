package com.sicms.service;

import com.sicms.dto.ChangePasswordRequest;
import com.sicms.entity.User;
import com.sicms.exception.AuthException;
import com.sicms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updatePassword(User user, String newRawPassword) {
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }

        updatePassword(user, newPassword);
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        if (request == null) {
            throw new AuthException("Invalid change password request.");
        }
        String confirm = request.getConfirmNewPassword() != null ? request.getConfirmNewPassword() : request.getConfirmPassword();
        if (!request.getNewPassword().equals(confirm)) {
            throw new AuthException("New password and confirm password do not match.");
        }
        changePassword(userEmail, request.getCurrentPassword(), request.getNewPassword());
    }
}
