package com.sicms.security;

import com.sicms.entity.Role;
import com.sicms.entity.User;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.frontend.url:https://bhashyamgnt.vercel.app}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                                RoleRepository roleRepository,
                                                PasswordEncoder passwordEncoder,
                                                JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("Google OAuth login success attempt for email: {}", email);

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 success handler failed: Email attribute missing from Google principal");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=google_email_missing");
            return;
        }

        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("Found existing user record for Google login: {}", normalizedEmail);
        } else {
            // Auto-provision new Google authenticated user as FACULTY
            log.info("Auto-provisioning new user via Google OAuth2: {}", normalizedEmail);
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(name != null && !name.isBlank() ? name : normalizedEmail.split("@")[0]);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setAccountEnabled(true);

            // Assign FACULTY role
            Optional<Role> roleOpt = roleRepository.findByRoleName("ROLE_FACULTY");
            if (roleOpt.isEmpty()) {
                roleOpt = roleRepository.findByRoleName("FACULTY");
            }
            roleOpt.ifPresent(user::setRole);

            user = userRepository.save(user);
            log.info("Successfully provisioned new Google user: ID={}, email={}", user.getId(), user.getEmail());
        }

        if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
            log.warn("Disabled user tried logging in via Google: {}", normalizedEmail);
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=account_disabled");
            return;
        }

        String token = jwtService.generateAccessToken(user);
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "ROLE_FACULTY";

        log.info("Google authentication verified successfully for user: {} ({})", user.getEmail(), roleName);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-success")
                .queryParam("token", token)
                .queryParam("role", roleName)
                .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(name != null ? name : user.getFullName(), StandardCharsets.UTF_8))
                .build(true).toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
