package com.sicms.controller;

import com.sicms.entity.User;
import com.sicms.repository.UserRepository;
import com.sicms.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:1078205301117-2u5nd4rn8hraa9jjo8hij2r2i0htcsac.apps.googleusercontent.com}")
    private String googleClientId;

    @Value("${app.frontend.url:https://bhashyamgnt.vercel.app}")
    private String frontendUrl;

    @Value("${app.admin.email:bhashyamgnt.edu@gmail.com}")
    private String adminEmail;

    public OAuth2Controller(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/authorization/google")
    public void redirectToGoogleAuthorization(HttpServletResponse response) throws IOException {
        System.out.println(">>> GOOGLE OAUTH INITIATED: client_id=" + googleClientId);

        // Check if real Google Client ID is configured
        if (googleClientId != null 
                && !googleClientId.isBlank() 
                && !googleClientId.contains("your_google_client_id_here")
                && !googleClientId.contains("placeholder")) {
            
            // Redirect to real Google OAuth2 Account Chooser
            String googleAuthUrl = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", googleClientId)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "email profile openid")
                    .queryParam("redirect_uri", "http://localhost:8080/login/oauth2/code/google")
                    .queryParam("prompt", "select_account")
                    .build().toUriString();

            response.sendRedirect(googleAuthUrl);
            return;
        }

        // Fallback for Development/Testing: authenticate admin email
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(adminEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtService.generateAccessToken(user);
            String role = user.getRole() != null ? user.getRole().getRoleName() : "ROLE_ADMIN";

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", token)
                    .queryParam("role", role)
                    .queryParam("email", user.getEmail())
                    .build().toUriString();

            response.sendRedirect(redirectUrl);
            return;
        }

        // Default error redirect
        response.sendRedirect(frontendUrl + "/login?error=google_unauthorized");
    }
}
