package com.sicms.service;

import com.sicms.dto.ChangePasswordRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AuthService authService;

    public UserService(AuthService authService) {
        this.authService = authService;
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        authService.changePassword(userEmail, request);
    }
}
