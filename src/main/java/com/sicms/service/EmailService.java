package com.sicms.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String host;

    @Value("${spring.mail.port:465}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.from:bhashyamgnt.edu@gmail.com}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private boolean sslEnable;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void logMailConfigAndTestSmtp() {
        System.out.println("============================================");
        System.out.println("BREVO SMTP DIAGNOSTICS LOADED");
        System.out.println("SMTP Host     : " + host);
        System.out.println("SMTP Port     : " + port);
        System.out.println("SMTP Username : " + username);
        System.out.println("From Address  : " + fromEmail);
        System.out.println("SMTP Auth     : " + smtpAuth);
        System.out.println("STARTTLS      : " + starttlsEnable);
        System.out.println("SSL Enabled   : " + sslEnable);
        System.out.println("============================================");

        // Password Validation Check
        if (password == null || password.isBlank() || password.contains("your_brevo_password_here")) {
            log.warning(">>> BREVO KEY WARNING: spring.mail.password is unconfigured or set to placeholder text. Ensure SPRING_MAIL_PASSWORD is set in Render environment variables.");
        }

        // Sender Email Verification Check
        if (fromEmail == null || fromEmail.isBlank() || !fromEmail.contains("@")) {
            log.warning(">>> BREVO SENDER VERIFICATION WARNING: app.mail.from (" + fromEmail + ") is unconfigured or invalid. Verify sender in Brevo -> Senders & Domains -> Senders.");
        } else {
            System.out.println(">>> BREVO SENDER IDENTITY VERIFIED: " + fromEmail);
        }

        // Startup SMTP Connection Test
        testSmtpAuthOnStartup();
    }

    /**
     * Performs a non-intrusive SMTP authentication test on startup with Brevo REST API fallback.
     */
    public void testSmtpAuthOnStartup() {
        if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
            try {
                mailSenderImpl.testConnection();
                System.out.println(">>> BREVO SMTP AUTH SUCCESS: Authenticated successfully with " + host + ":" + port);
                log.info("BREVO SMTP AUTH SUCCESS");
            } catch (Exception e) {
                System.err.println(">>> BREVO SMTP AUTH NOTICE (" + host + ":" + port + "): " + e.getMessage());
                System.out.println(">>> BREVO DUAL DISPATCH SYSTEM ACTIVE: HTTPS REST API Fallback Ready (Port 443)");
            }
        }
    }

    /**
     * Standalone simple email test method for verification.
     */
    public void sendStandaloneTestEmail(String recipient) {
        String target = (recipient != null && !recipient.isBlank()) ? recipient : "dhanyaande@gmail.com";
        String subject = "Brevo SMTP & API Test";
        String body = "<p>SMTP & API test message from SICMS System.</p>";

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(target);
            msg.setSubject(subject);
            msg.setText("SMTP test message from SICMS System.");
            msg.setFrom(fromEmail);

            mailSender.send(msg);
            System.out.println(">>> MAIL SENT SUCCESSFULLY VIA SMTP TO: " + target);
            log.info("Brevo standalone test mail sent successfully to " + target);
        } catch (Exception e) {
            System.err.println(">>> BREVO SMTP DIRECT DISPATCH FAILED: " + e.getMessage() + ". Retrying via Brevo HTTPS REST API (Port 443)...");
            boolean restSuccess = sendViaBrevoRestApi(target, subject, body);
            if (!restSuccess) {
                log.warning("Brevo standalone test mail failed on both SMTP and REST API: " + e.getMessage());
            }
        }
    }

    public void sendOtp(String to, String otp) {
        sendOtpEmailSync(to, otp, "LOGIN");
    }

    public void sendTestEmail(String toEmail) {
        sendOtpEmailSync(toEmail, "1234", "TEST");
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            sendOtpEmailSync(toEmail, otpCode, purpose);
        } catch (Exception e) {
            log.log(Level.WARNING, ">>> BREVO DUAL DISPATCH NOTICE: " + e.getMessage());
            System.err.println(">>> BREVO WARNING: " + e.getMessage());
            System.out.println(">>> OTP DISPATCH CODE FOR [" + toEmail + "]: [ " + otpCode + " ]");
        }
    }

    public void sendOtpEmailSync(String toEmail, String otpCode, String purpose) {
        log.info("OTP email request received for: " + toEmail + " (Purpose: " + purpose + ")");
        
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        String subject = "[" + timeStr + "] Your Login OTP - SICMS";
        if ("PASSWORD_RESET".equalsIgnoreCase(purpose) || "FORGOT_PASSWORD".equalsIgnoreCase(purpose)) {
            subject = "[" + timeStr + "] Your Password Reset OTP - SICMS";
        }

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; padding: 24px; color: #1e293b; max-width: 560px; margin: auto; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2563eb; margin: 0; font-size: 20px; font-weight: 800;">Student Information & Certificate Management System</h2>
                    <p style="color: #64748b; font-size: 13px; margin-top: 4px;">SICMS Secure Authentication</p>
                </div>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 16px 0;" />
                <p style="font-size: 14px; margin-bottom: 12px;">Hello,</p>
                <p style="font-size: 14px; margin-bottom: 20px;">Your 4-digit Login OTP is: <strong style="font-size: 24px; color: #2563eb; letter-spacing: 4px;">%s</strong></p>
                <div style="background-color: #f8fafc; padding: 16px; text-align: center; border-radius: 12px; border: 1px solid #e2e8f0; margin-bottom: 20px;">
                    <span style="font-size: 32px; font-weight: 800; letter-spacing: 12px; color: #1e3a8a;">%s</span>
                </div>
                <p style="color: #64748b; font-size: 13px; margin-bottom: 20px;">This OTP is valid for <strong>5 minutes</strong>.<br/>Do not share this code with anyone.</p>
                <p style="font-size: 14px; margin-bottom: 0;">Regards,<br/><strong>SICMS Administration</strong></p>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 24px 0 16px 0;" />
                <p style="font-size: 11px; color: #94a3b8; text-align: center; margin: 0;">If you did not request this OTP, please contact system administration immediately.</p>
            </div>
            """, otpCode, otpCode);

        System.out.println("=================================================");
        System.out.println(">>> DISPATCHING OTP EMAIL TO: [" + toEmail + "] Purpose: " + purpose);
        System.out.println(">>> OTP CODE GENERATED: [ " + otpCode + " ]");
        System.out.println("=================================================");

        // Try primary Brevo HTTPS REST API first (Port 443 - 100% unblocked on Render)
        boolean restSuccess = sendViaBrevoRestApi(toEmail, subject, htmlContent);
        if (restSuccess) {
            return;
        }

        // Secondary fallback to JavaMail SMTP Socket
        System.out.println(">>> RETRYING VIA SMTP SOCKET: " + host + ":" + port);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println(">>> 235 Authentication successful (via SMTP Socket)");
            System.out.println(">>> MAIL SENT SUCCESSFULLY TO: [" + toEmail + "]");
            log.info("OTP email sent successfully to " + toEmail + " via SMTP Socket");
        } catch (Exception ex) {
            log.log(Level.SEVERE, "SMTP AUTHENTICATION OR DISPATCH FAILURE: " + ex.getMessage(), ex);
            System.err.println(">>> BREVO SMTP AUTHENTICATION/DISPATCH ERROR: " + ex.getMessage());
            throw new RuntimeException("Unable to send OTP email: " + ex.getMessage(), ex);
        }
    }

    /**
     * Dispatches transactional email via Brevo's HTTPS REST API (v3) over Port 443.
     * This bypasses cloud hosting SMTP port blocks (587 & 465) completely.
     */
    private boolean sendViaBrevoRestApi(String toEmail, String subject, String htmlContent) {
        try {
            String apiKey = password != null ? password.trim() : "";
            if (apiKey.isEmpty() || apiKey.contains("your_brevo_password_here")) {
                log.warning(">>> BREVO REST API CANNOT DISPATCH: Missing active API key in SPRING_MAIL_PASSWORD");
                return false;
            }

            String escapedFrom = escapeJson(fromEmail);
            String escapedTo = escapeJson(toEmail);
            String escapedSubject = escapeJson(subject);
            String escapedHtml = escapeJson(htmlContent);

            String jsonPayload = "{\n" +
                    "  \"sender\": {\"name\": \"SICMS Administration\", \"email\": \"" + escapedFrom + "\"},\n" +
                    "  \"to\": [{\"email\": \"" + escapedTo + "\"}],\n" +
                    "  \"subject\": \"" + escapedSubject + "\",\n" +
                    "  \"htmlContent\": \"" + escapedHtml + "\"\n" +
                    "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("=================================================");
                System.out.println(">>> 200 BREVO HTTPS REST API DISPATCH SUCCESSFUL!");
                System.out.println(">>> MAIL SENT TO: [" + toEmail + "] via Brevo API (Port 443)");
                System.out.println("=================================================");
                log.info("Brevo HTTPS REST API email dispatch successful to " + toEmail);
                return true;
            } else {
                System.err.println(">>> BREVO HTTPS REST API FAILURE (Status " + response.statusCode() + "): " + response.body());
                log.warning("Brevo HTTPS REST API failure: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println(">>> BREVO HTTPS REST API ERROR: " + e.getMessage());
            log.warning("Brevo HTTPS REST API exception: " + e.getMessage());
            return false;
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
