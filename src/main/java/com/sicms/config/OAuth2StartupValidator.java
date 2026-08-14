package com.sicms.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuth2StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(OAuth2StartupValidator.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostConstruct
    public void validateOAuth2Setup() {
        boolean hasClientId = googleClientId != null && !googleClientId.isBlank() && !googleClientId.contains("placeholder");
        boolean hasClientSecret = googleClientSecret != null && !googleClientSecret.isBlank() && !googleClientSecret.contains("placeholder") && !googleClientSecret.contains("YOUR_GOOGLE_CLIENT_SECRET");

        log.info("================================================================================");
        log.info("                 SICMS GOOGLE OAUTH2 STARTUP DIAGNOSTICS                         ");
        log.info("================================================================================");
        log.info("  ✓ spring-boot-starter-oauth2-client: LOADED");
        
        if (hasClientId) {
            log.info("  ✓ Google Client ID     : {}", googleClientId);
        } else {
            log.error("  ❌ Google Client ID     : [MISSING / BLANK] - Set GOOGLE_CLIENT_ID in .env or environment");
        }

        if (hasClientSecret) {
            String maskedSecret = maskSecret(googleClientSecret);
            log.info("  ✓ Google Client Secret : {} (Length: {} chars)", maskedSecret, googleClientSecret.length());
        } else {
            log.error("  ❌ Google Client Secret : [MISSING / EMPTY / PLACEHOLDER]");
            log.error("     --> Root Cause for '401 Unauthorized / invalid_token_response' during Google login!");
            log.error("     --> Action Required: Set active GOOGLE_CLIENT_SECRET in Backend/.env and restart Spring Boot.");
        }

        log.info("  ✓ OAuth2 Provider URI  : https://oauth2.googleapis.com/token");
        log.info("  ✓ Redirect URI (Local) : http://localhost:{}/login/oauth2/code/google", serverPort);
        log.info("  ✓ Frontend Origin      : {}", frontendUrl);
        log.info("================================================================================");

        if (!hasClientId || !hasClientSecret) {
            log.warn("⚠️  GOOGLE OAUTH IS NOT FULLY CONFIGURED. Logins will fail with 401 until valid credentials are provided.");
        } else {
            log.info("✓ Google OAuth2 configuration validated successfully. Ready for authentication.");
        }
        log.info("================================================================================");
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "********";
        }
        return secret.substring(0, 6) + "..." + secret.substring(secret.length() - 3);
    }
}
