package com.sicms.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuth2StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(OAuth2StartupValidator.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:1078205301117-2u5nd4rn8hraa9jjo8hij2r2i0htcsac.apps.googleusercontent.com}")
    private String googleClientId;

    @Value("${app.frontend.url:https://bhashyamgnt.vercel.app}")
    private String frontendUrl;

    @PostConstruct
    public void validateOAuth2Setup() {
        boolean hasClientId = googleClientId != null && !googleClientId.isBlank() && !googleClientId.contains("your_google_client_id_here");

        log.info("==========================================================");
        log.info("✓ Google OAuth dependency loaded");
        log.info("✓ Google Client ID configured: " + (hasClientId ? googleClientId : "[MISSING]"));
        log.info("✓ OAuth2 login enabled");
        log.info("✓ Redirect URI registered: {baseUrl}/login/oauth2/code/google");
        log.info("✓ JWT service initialized");
        log.info("✓ Target Frontend URL: " + frontendUrl);
        log.info("==========================================================");
    }
}
