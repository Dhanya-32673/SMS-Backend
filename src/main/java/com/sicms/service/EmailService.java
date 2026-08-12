package com.sicms.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean sslEnable;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void logMailConfig() {
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
    }

    public void sendOtp(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your OTP Code");
            helper.setText("<h3>Your OTP is: <b>" + otp + "</b></h3>", true);

            mailSender.send(message);
            log.info("OTP email sent successfully to " + to);
        } catch (Exception e) {
            log.warning("BREVO SMTP WARNING: Failed to send OTP email: " + e.getMessage());
            System.out.println("=================================================");
            System.out.println(">>> FALLBACK OTP DISPATCH CODE FOR [" + to + "]: [ " + otp + " ]");
            System.out.println("=================================================");
        }
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
        System.out.println(">>> CONNECTING TO BREVO SMTP SERVER: " + host + ":" + port + " (STARTTLS=" + starttlsEnable + ", SSL=" + sslEnable + ")");
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
            log.info("OTP email sent successfully to " + toEmail);
            System.out.println(">>> BREVO SMTP CONNECTION SUCCESS: OTP EMAIL SENT TO: [" + toEmail + "]");
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to send OTP email via Brevo SMTP", ex);
            System.err.println(">>> BREVO SMTP ERROR: " + ex.getMessage());
            throw new RuntimeException("Unable to send OTP email: " + ex.getMessage(), ex);
        }
    }
}
