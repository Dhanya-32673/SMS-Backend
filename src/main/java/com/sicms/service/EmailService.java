package com.sicms.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final String apiKey;
    private final String fromAddress;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    public EmailService(
            @Value("${resend.api.key:your_resend_api_key_here}") String apiKey,
            @Value("${app.mail.from:onboarding@resend.dev}") String fromAddress
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromAddress = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress.trim() : "onboarding@resend.dev";
        log.info(">>> EMAIL SERVICE INITIALIZED with Sender: " + this.fromAddress);
    }

    public void sendOtp(String to, String otp) {
        sendOtpEmailSync(to, otp, "LOGIN");
    }

    public void sendTestEmail(String toEmail) {
        sendOtpEmailSync(toEmail, "123456", "TEST");
    }

    public void sendStandaloneTestEmail(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty.");
        }
        sendOtpEmailSync(recipient, "123456", "TEST");
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            sendOtpEmailSync(toEmail, otpCode, purpose);
        } catch (Exception e) {
            log.log(Level.WARNING, ">>> OTP DISPATCH WARNING for [" + toEmail + "]: " + e.getMessage(), e);
            System.err.println(">>> OTP DISPATCH WARNING for [" + toEmail + "]: " + e.getMessage());
            System.out.println(">>> OTP DISPATCH CODE FOR [" + toEmail + "]: [ " + otpCode + " ]");
        }
    }

    public void sendOtpEmail(String to, String otp) {
        sendOtpEmailSync(to, otp, "PASSWORD_RESET");
    }
    public void sendOtpEmailSync(String toEmail, String otpCode, String purpose) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Target user email cannot be empty for OTP delivery.");
        }

        String userEmail = toEmail.trim().toLowerCase();
        log.info("OTP email request received for: " + userEmail + " (Purpose: " + purpose + ")");

        boolean isReset = "PASSWORD_RESET".equalsIgnoreCase(purpose) || "FORGOT_PASSWORD".equalsIgnoreCase(purpose);
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        String subject = isReset 
                ? "[" + timeStr + "] Your Password Reset OTP - SICMS"
                : "[" + timeStr + "] Your Login OTP - SICMS";

        String title = isReset ? "Password Reset Verification" : "Login Verification";
        String otpLabel = isReset ? "6-digit Password Reset OTP" : "OTP Code";

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; padding: 24px; color: #1e293b; max-width: 560px; margin: auto; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2563eb; margin: 0; font-size: 20px; font-weight: 800;">Student Information & Certificate Management System</h2>
                    <p style="color: #64748b; font-size: 13px; margin-top: 4px;">SICMS Secure Authentication — %s</p>
                </div>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 16px 0;" />
                <p style="font-size: 14px; margin-bottom: 12px;">Hello,</p>
                <p style="font-size: 14px; margin-bottom: 20px;">Your %s is: <strong style="font-size: 24px; color: #2563eb; letter-spacing: 4px;">%s</strong></p>
                <div style="background-color: #f8fafc; padding: 16px; text-align: center; border-radius: 12px; border: 1px solid #e2e8f0; margin-bottom: 20px;">
                    <span style="font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #1e3a8a;">%s</span>
                </div>
                <p style="color: #64748b; font-size: 13px; margin-bottom: 20px;">This code is valid for <strong>5 minutes</strong>.<br/>Do not share this OTP with anyone for security reasons.</p>
                <p style="font-size: 14px; margin-bottom: 0;">Regards,<br/><strong>SICMS Administration</strong></p>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 24px 0 16px 0;" />
                <p style="font-size: 11px; color: #94a3b8; text-align: center; margin: 0;">If you did not request this OTP, please ignore this email or contact support.</p>
            </div>
            """, title, otpLabel, otpCode, otpCode);

        System.out.println("=================================================");
        System.out.println(">>> RESEND/SMTP REQUESTED FOR EMAIL: [" + userEmail + "]");
        System.out.println(">>> OTP GENERATED FOR EMAIL: [" + userEmail + "] Code: [ " + otpCode + " ]");
        System.out.println(">>> DISPATCHING EMAIL FROM [" + fromAddress + "] TO [" + userEmail + "]");
        System.out.println("=================================================");

        // Try JavaMailSender if configured
        boolean sentViaSmtp = false;
        if (javaMailSender != null) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(userEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                javaMailSender.send(mimeMessage);
                sentViaSmtp = true;
                log.info("JavaMailSender dispatch successful to " + userEmail);
            } catch (Exception smtpEx) {
                log.log(Level.FINE, "JavaMailSender send failed, falling back to Resend API: " + smtpEx.getMessage());
            }
        }

        if (sentViaSmtp) {
            return;
        }

        // Try Resend API SDK
        try {
            if (apiKey.isEmpty() || apiKey.contains("your_resend_api_key")) {
                throw new IllegalStateException("RESEND_API_KEY is not configured in environment.");
            }

            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(userEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse data = resend.emails().send(params);

            System.out.println("=================================================");
            System.out.println(">>> 200 RESEND API DISPATCH SUCCESSFUL! ID: " + (data != null ? data.getId() : "OK"));
            System.out.println(">>> EMAIL SENT TO EMAIL: [" + userEmail + "]");
            System.out.println("=================================================");
            log.info("Resend API email dispatch successful to " + userEmail);
        } catch (Exception e) {
            System.err.println(">>> DISPATCH ERROR for [" + userEmail + "]: " + e.getMessage());
            log.log(Level.SEVERE, "Email dispatch failure for [" + userEmail + "]: " + e.getMessage(), e);
            throw new RuntimeException("Unable to send OTP email: " + e.getMessage(), e);
        }
    }
}
