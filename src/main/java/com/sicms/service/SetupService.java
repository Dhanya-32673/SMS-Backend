package com.sicms.service;

import com.sicms.dto.SetupRequest;
import com.sicms.dto.UserDto;
import com.sicms.entity.AuthProvider;
import com.sicms.entity.Role;
import com.sicms.entity.User;
import com.sicms.exception.AuthException;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService {

    private static final Logger log = LoggerFactory.getLogger(SetupService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public SetupService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        AuthService authService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public boolean adminExists() {
        return userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() != null &&
                        ("ROLE_ADMIN".equalsIgnoreCase(u.getRole().getRoleName()) ||
                         "ADMIN".equalsIgnoreCase(u.getRole().getRoleName())));
    }

    @Transactional
    public UserDto createInitialAdmin(SetupRequest request) {
        if (adminExists()) {
            throw new AccessDeniedException("Initial admin setup has already been completed. An admin user already exists.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Password and confirm password do not match.");
        }

        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new AuthException("An account already exists with email: " + cleanEmail);
        }

        authService.validatePasswordPolicy(request.getPassword());

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator")));

        User admin = new User();
        admin.setFullName(request.getFullName().trim());
        admin.setEmail(cleanEmail);
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(adminRole);
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setEmailVerified(true);
        admin.setAccountEnabled(true);

        User saved = userRepository.save(admin);
        log.info(">>> INITIAL ADMIN CREATED SUCCESSFULLY for email={}", cleanEmail);
        return new UserDto(saved);
    }
}
