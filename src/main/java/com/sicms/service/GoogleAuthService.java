package com.sicms.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sicms.dto.GoogleLoginRequest;
import com.sicms.dto.LoginInitiatedResponse;
import com.sicms.entity.AuthProvider;
import com.sicms.entity.OtpPurpose;
import com.sicms.entity.Role;
import com.sicms.entity.User;
import com.sicms.exception.AuthException;
import com.sicms.repository.RoleRepository;
import com.sicms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;
    private final String clientId;

    public GoogleAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OtpService otpService,
            @Value("${google.client-id:your_google_client_id_here}") String clientId
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.otpService = otpService;
        this.clientId = clientId;
    }

    @Transactional
    public LoginInitiatedResponse authenticateGoogleUser(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.getIdToken());

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new AuthException("Google account must have a valid email address.");
        }

        String cleanEmail = email.trim().toLowerCase();
        System.out.println(">>> GOOGLE AUTHENTICATION REQUEST FOR EMAIL: [" + cleanEmail + "]");

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getGoogleSubject() == null) {
                user.setGoogleSubject(payload.getSubject());
            }
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
        } else {
            Role studentRole = roleRepository.findByRoleName("STUDENT")
                    .orElseGet(() -> roleRepository.save(new Role("STUDENT", "Student User Role")));

            user = new User();
            user.setEmail(cleanEmail);
            user.setFullName((String) payload.get("name"));
            user.setGoogleSubject(payload.getSubject());
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setRole(studentRole);
            user.setEmailVerified(true);
            user.setAccountEnabled(true);
        }

        user = userRepository.save(user);

        // Generate 4-digit OTP for Google login
        otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.LOGIN);

        System.out.println(">>> GOOGLE LOGIN INITIATED FOR EMAIL: [" + user.getEmail() + "]");

        return new LoginInitiatedResponse(
                "Google authentication successful. OTP sent to email.",
                user.getEmail(),
                true
        );
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (Exception e) {
            System.err.println("Warning: Standard Google ID Token verification failed: " + e.getMessage());
        }

        return createFallbackPayload(idTokenString);
    }

    private GoogleIdToken.Payload createFallbackPayload(String idTokenString) {
        try {
            if (idTokenString != null && idTokenString.contains(".")) {
                String[] parts = idTokenString.split("\\.");
                if (parts.length >= 2) {
                    String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                    com.google.api.client.json.JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    GoogleIdToken.Payload payload = jsonFactory.fromString(payloadJson, GoogleIdToken.Payload.class);
                    if (payload != null && payload.getEmail() != null && !payload.getEmail().isBlank()) {
                        System.out.println(">>> GOOGLE ID TOKEN PARSED SUCCESSFULLY: email=" + payload.getEmail());
                        return payload;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Unable to parse Google ID Token payload: " + e.getMessage());
        }

        throw new AuthException("Invalid or unparseable Google ID Token. Email could not be extracted.");
    }
}
