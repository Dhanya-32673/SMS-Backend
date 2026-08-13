package com.sicms.security;

import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
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

    @Value("${app.frontend.url:https://bhashyamgnt.vercel.app}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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
            log.warn("Unauthorized Google login attempt: {}", normalizedEmail);
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", "google_not_authorized")
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

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        boolean isAuthorizedRole = roleName.contains("ADMIN") || roleName.contains("FACULTY");

        if (!isAuthorizedRole) {
            log.warn("Google login rejected - Unauthorized role '{}': {}", roleName, normalizedEmail);
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=role_not_allowed");
            return;
        }

        String token = jwtService.generateAccessToken(user);

        log.info("Google login success: {}", normalizedEmail);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-success")
                .queryParam("token", token)
                .queryParam("role", roleName)
                .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(name != null ? name : user.getFullName(), StandardCharsets.UTF_8))
                .build(true).toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
