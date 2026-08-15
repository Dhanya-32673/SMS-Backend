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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final String apiKey;
    private final String fromAddress;
    private final String adminEmail;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    public EmailService(
            @Value("${resend.api.key:}") String apiKey,
            @Value("${app.mail.from:onboarding@resend.dev}") String fromAddress,
            @Value("${app.admin.email:bhashyamgnt.edu@gmail.com}") String adminEmail
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromAddress = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress.trim() : "onboarding@resend.dev";
        this.adminEmail = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail.trim() : "admin@college.edu";
        log.info(">>> EMAIL SERVICE INITIALIZED with Sender: " + this.fromAddress + " | Admin Target: " + this.adminEmail);
    }

    public String getAdminEmail() {
        return adminEmail;
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

    @Async
    public void sendFacultyResetOtpToAdmin(
            String facultyName,
            String facultyEmail,
            String employeeId,
            String otpCode,
            String reason,
            LocalDateTime requestedAt,
            LocalDateTime expiryAt
    ) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a");
        String reqTime = requestedAt != null ? requestedAt.format(fmt) : LocalDateTime.now().format(fmt);
        String expTime = expiryAt != null ? expiryAt.format(fmt) : LocalDateTime.now().plusMinutes(10).format(fmt);

        String subject = "[ACTION REQUIRED] Faculty Password Reset OTP - " + (facultyName != null ? facultyName : facultyEmail);

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; padding: 24px; color: #1e293b; max-width: 600px; margin: auto; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #dc2626; margin: 0; font-size: 20px; font-weight: 800;">Administrator Action Required</h2>
                    <p style="color: #64748b; font-size: 13px; margin-top: 4px;">Faculty Password Reset Authorization Request</p>
                </div>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 16px 0;" />
                <p style="font-size: 14px;">A faculty member has requested a password reset. Below are the details:</p>
                <table style="width: 100%%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px;">
                    <tr><td style="padding: 6px 0; color: #64748b;">Faculty Name:</td><td style="padding: 6px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 6px 0; color: #64748b;">Faculty Email:</td><td style="padding: 6px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 6px 0; color: #64748b;">Employee ID:</td><td style="padding: 6px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 6px 0; color: #64748b;">Reason:</td><td style="padding: 6px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 6px 0; color: #64748b;">Requested At:</td><td style="padding: 6px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 6px 0; color: #64748b;">Expires At:</td><td style="padding: 6px 0; font-weight: bold; color: #dc2626;">%s</td></tr>
                </table>
                <div style="background-color: #fef2f2; padding: 16px; text-align: center; border-radius: 12px; border: 1px solid #fecaca; margin-bottom: 20px;">
                    <span style="font-size: 12px; color: #991b1b; display: block; margin-bottom: 6px; font-weight: bold;">6-DIGIT APPROVAL OTP FOR ADMIN USE ONLY</span>
                    <span style="font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #991b1b;">%s</span>
                </div>
                <p style="color: #64748b; font-size: 13px; margin-bottom: 0;">This OTP code is valid for <strong>10 minutes</strong>. Please verify the request before approving.</p>
            </div>
            """,
                facultyName != null ? facultyName : "N/A",
                facultyEmail != null ? facultyEmail : "N/A",
                employeeId != null && !employeeId.isBlank() ? employeeId : "N/A",
                reason != null && !reason.isBlank() ? reason : "Not specified",
                reqTime,
                expTime,
                otpCode
        );

        dispatchGenericHtmlEmail(adminEmail, subject, htmlContent);
    }

    @Async
    public void sendFacultyPasswordResetConfirmation(String facultyEmail) {
        String subject = "Your Password Has Been Reset - SICMS Administration";
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; padding: 24px; color: #1e293b; max-width: 560px; margin: auto; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <h2 style="color: #16a34a; margin: 0 0 12px 0; font-size: 20px; font-weight: 800;">Password Updated Successfully</h2>
                <p style="font-size: 14px; margin-bottom: 16px;">Hello,</p>
                <p style="font-size: 14px; margin-bottom: 20px;">Your SICMS faculty account password was successfully reset by System Administration.</p>
                <p style="font-size: 14px; margin-bottom: 20px;">You can now sign in with your new password on the SICMS login portal.</p>
                <p style="color: #64748b; font-size: 12px; margin-bottom: 0;">If you did not request this change, please contact system administration immediately.</p>
            </div>
            """);

        dispatchGenericHtmlEmail(facultyEmail, subject, htmlContent);
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

        dispatchGenericHtmlEmail(userEmail, subject, htmlContent);
    }

    private void dispatchGenericHtmlEmail(String targetEmail, String subject, String htmlContent) {
        if (targetEmail == null || targetEmail.isBlank()) return;
        String email = targetEmail.trim().toLowerCase();

        boolean sentViaSmtp = false;
        if (javaMailSender != null) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                javaMailSender.send(mimeMessage);
                sentViaSmtp = true;
                log.info("JavaMailSender dispatch successful to " + email);
            } catch (Exception smtpEx) {
                log.log(Level.FINE, "JavaMailSender send failed, falling back to Resend API: " + smtpEx.getMessage());
            }
        }

        if (sentViaSmtp) {
            return;
        }

        try {
            if (apiKey.isEmpty() || apiKey.contains("your_resend_api_key")) {
                throw new IllegalStateException("RESEND_API_KEY is not configured in environment.");
            }

            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(email)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse data = resend.emails().send(params);
            log.info("Resend API email dispatch successful to " + email + " [ID: " + (data != null ? data.getId() : "OK") + "]");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Email dispatch failure for [" + email + "]: " + e.getMessage(), e);
        }
    }
}
