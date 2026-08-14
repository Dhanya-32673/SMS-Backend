package com.sicms.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        log.error("================================================================================");
        log.error("❌ GOOGLE OAUTH AUTHENTICATION FAILURE DETECTED");
        log.error("================================================================================");
        log.error("  Error Message : {}", exception.getMessage());
        log.error("  Exception Type: {}", exception.getClass().getName());
        log.error("  Request URI   : {}", request.getRequestURI());
        log.error("  Query String  : {}", request.getQueryString());

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            log.error("  OAuth2 Code   : {}", error.getErrorCode());
            log.error("  OAuth2 Desc   : {}", error.getDescription());
            log.error("  OAuth2 URI    : {}", error.getUri());
        }

        if (exception.getCause() != null) {
            log.error("  Root Cause    : {}", exception.getCause().toString());
        }

        // Diagnosis hints
        String msg = exception.getMessage() != null ? exception.getMessage() : "";
        if (msg.contains("invalid_token_response") || msg.contains("401")) {
            log.error("--------------------------------------------------------------------------------");
            log.error("💡 DIAGNOSIS: 401 Unauthorized during token exchange!");
            log.error("   1. Google rejected the GOOGLE_CLIENT_SECRET.");
            log.error("   2. Check Backend/.env -> ensure GOOGLE_CLIENT_SECRET is NOT empty and matches the ACTIVE secret from Google Cloud Console.");
            log.error("   3. In Google Cloud Console, verify Authorized redirect URI is:");
            log.error("      http://localhost:8080/login/oauth2/code/google");
            log.error("--------------------------------------------------------------------------------");
        } else if (msg.contains("access_denied")) {
            log.warn("💡 DIAGNOSIS: User cancelled or was denied access on Google consent screen.");
        }
        log.error("================================================================================");

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", "google_failed")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
