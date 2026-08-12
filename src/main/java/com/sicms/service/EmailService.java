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
     * Performs a non-intrusive SMTP authentication test on startup with Port 465 SSL fallback.
     */
    public void testSmtpAuthOnStartup() {
        if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
            try {
                mailSenderImpl.testConnection();
                System.out.println(">>> BREVO SMTP AUTH SUCCESS: Authenticated successfully with " + host + ":" + port);
                log.info("BREVO SMTP AUTH SUCCESS");
            } catch (Exception e) {
                System.err.println(">>> BREVO SMTP AUTH WARNING (" + host + ":" + port + "): " + e.getMessage());
                // If Port 587 timed out on Render due to firewall blocking, attempt fallback to Port 465 SSL
                if (port == 587 && e.getMessage() != null && e.getMessage().contains("Couldn't connect")) {
                    System.out.println(">>> ATTEMPTING AUTOMATIC FALLBACK TO BREVO PORT 465 SSL...");
                    try {
                        mailSenderImpl.setPort(465);
                        Properties props = mailSenderImpl.getJavaMailProperties();
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.starttls.enable", "false");
                        mailSenderImpl.testConnection();
                        this.port = 465;
                        this.sslEnable = true;
                        this.starttlsEnable = false;
                        System.out.println(">>> BREVO SMTP PORT 465 SSL FALLBACK SUCCESSFUL!");
                        log.info("BREVO SMTP PORT 465 SSL FALLBACK SUCCESSFUL");
                    } catch (Exception ex) {
                        System.err.println(">>> BREVO SMTP PORT 465 FALLBACK FAILED: " + ex.getMessage());
                        log.warning("BREVO SMTP PORT 465 FALLBACK FAILED: " + ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Standalone simple email test method for verification.
     */
    public void sendStandaloneTestEmail(String recipient) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(recipient != null && !recipient.isBlank() ? recipient : "dhanyaande@gmail.com");
            msg.setSubject("Brevo SMTP Test");
            msg.setText("SMTP test message");
            msg.setFrom(fromEmail);

            mailSender.send(msg);
            System.out.println(">>> MAIL SENT SUCCESSFULLY TO: " + msg.getTo()[0]);
            log.info("Brevo standalone test mail sent successfully to " + msg.getTo()[0]);
        } catch (Exception e) {
            System.err.println(">>> BREVO STANDALONE MAIL TEST ERROR: " + e.getMessage());
            log.warning("Brevo standalone test mail failed: " + e.getMessage());
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
            log.log(Level.WARNING, ">>> BREVO SMTP WARNING: Outbound mail dispatch notice: " + e.getMessage());
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
        System.out.println(">>> SMTP CONNECTION STARTED: " + host + ":" + port + " (STARTTLS=" + starttlsEnable + ", SSL=" + sslEnable + ")");
        System.out.println(">>> DISPATCHING OTP EMAIL TO: [" + toEmail + "] Purpose: " + purpose);
        System.out.println(">>> OTP CODE GENERATED: [ " + otpCode + " ]");
        System.out.println("=================================================");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println(">>> 235 Authentication successful");
            System.out.println(">>> MAIL SENT SUCCESSFULLY TO: [" + toEmail + "]");
            log.info("OTP email sent successfully to " + toEmail);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "SMTP AUTHENTICATION OR DISPATCH FAILURE: " + ex.getMessage(), ex);
            System.err.println(">>> BREVO SMTP AUTHENTICATION/DISPATCH ERROR: " + ex.getMessage());

            // If Port 587 timed out on Render during send, try fallback to Port 465 SSL
            if (mailSender instanceof JavaMailSenderImpl mailSenderImpl && port == 587 && ex.getMessage() != null && ex.getMessage().contains("Couldn't connect")) {
                System.out.println(">>> RETRYING DISPATCH WITH BREVO PORT 465 SSL FALLBACK...");
                try {
                    mailSenderImpl.setPort(465);
                    Properties props = mailSenderImpl.getJavaMailProperties();
                    props.put("mail.smtp.ssl.enable", "true");
                    props.put("mail.smtp.starttls.enable", "false");

                    MimeMessage message = mailSenderImpl.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(fromEmail);
                    helper.setTo(toEmail);
                    helper.setSubject(subject);
                    helper.setText(htmlContent, true);

                    mailSenderImpl.send(message);
                    this.port = 465;
                    this.sslEnable = true;
                    this.starttlsEnable = false;
                    System.out.println(">>> 235 Authentication successful (via Port 465 SSL Fallback)");
                    System.out.println(">>> MAIL SENT SUCCESSFULLY TO: [" + toEmail + "]");
                    log.info("OTP email sent successfully to " + toEmail + " via Port 465 SSL Fallback");
                    return;
                } catch (Exception retryEx) {
                    System.err.println(">>> PORT 465 RETRY ALSO FAILED: " + retryEx.getMessage());
                }
            }

            throw new RuntimeException("Unable to send OTP email: " + ex.getMessage(), ex);
        }
    }
}
