package com.sicms.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PerformanceLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PerformanceLoggingFilter.class);
    private static final long SLOW_REQUEST_THRESHOLD_MS = 500;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Skip logging for health checks, HEAD requests to root, and static assets
        if (isExcludedPath(method, uri)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("[PERF WARN] Slow Business API Execution: {} {} -> Status {} in {} ms (> 500ms threshold)",
                        method, uri, status, duration);
            }
        }
    }

    private boolean isExcludedPath(String method, String uri) {
        if ("HEAD".equalsIgnoreCase(method) && "/".equals(uri)) return true;
        return "/".equals(uri) ||
               "/health".equals(uri) ||
               "/api/health".equals(uri) ||
               "/favicon.ico".equals(uri) ||
               uri.startsWith("/actuator/") ||
               uri.startsWith("/h2-console");
    }
}
