package com.sicms.security;

import com.sicms.entity.OtpPurpose;
import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.service.OtpService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                                JwtService jwtService,
                                                OtpService otpService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("Google login attempt: {}", email);

        if (email == null || email.isBlank()) {
            log.warn("Unauthorized Google login attempt: missing email attribute");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=google_email_missing");
            return;
        }

        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isEmpty()) {
            log.warn("Unauthorized Google login attempt - user not found in database: {}", normalizedEmail);
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", "unauthorized")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        User user = existingUser.get();

        if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
            log.warn("Google login rejected - Account disabled: {}", normalizedEmail);
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=account_disabled");
            return;
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName().toUpperCase() : "";

        // Admin Role: Requires 2FA OTP verification before issuing final JWT
        if (roleName.contains("ADMIN")) {
            log.info("Google login verified for ADMIN: {}. Triggering OTP verification.", normalizedEmail);
            try {
                otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.LOGIN);
            } catch (Exception e) {
                log.error("Failed to generate OTP for admin: " + e.getMessage(), e);
            }

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/admin-otp")
                    .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                    .queryParam("name", URLEncoder.encode(name != null ? name : user.getFullName(), StandardCharsets.UTF_8))
                    .build(true).toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // Faculty or Student Role: Direct Login with JWT
        if (roleName.contains("FACULTY") || roleName.contains("STUDENT")) {
            String token = jwtService.generateAccessToken(user);
            log.info("Google login success for {}: Direct JWT issued", normalizedEmail);

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-success")
                    .queryParam("token", token)
                    .queryParam("role", roleName)
                    .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                    .queryParam("name", URLEncoder.encode(name != null ? name : user.getFullName(), StandardCharsets.UTF_8))
                    .build(true).toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // Other unauthorized roles
        log.warn("Google login rejected - Role '{}' not allowed: {}", roleName, normalizedEmail);
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=role_not_allowed");
    }
}
