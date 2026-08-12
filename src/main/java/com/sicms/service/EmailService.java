package com.sicms.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
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

    public EmailService(
            @Value("${resend.api.key:your_resend_api_key_here}") String apiKey,
            @Value("${app.mail.from:onboarding@resend.dev}") String fromAddress
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromAddress = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress.trim() : "onboarding@resend.dev";
        log.info(">>> RESEND EMAIL SERVICE INITIALIZED with Sender: " + this.fromAddress);
    }

    public void sendOtp(String to, String otp) {
        sendOtpEmailSync(to, otp, "LOGIN");
    }

    public void sendTestEmail(String toEmail) {
        sendOtpEmailSync(toEmail, "1234", "TEST");
    }

    public void sendStandaloneTestEmail(String recipient) {
        String target = (recipient != null && !recipient.isBlank()) ? recipient : "dhanyaande@gmail.com";
        sendOtpEmailSync(target, "1234", "TEST");
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            sendOtpEmailSync(toEmail, otpCode, purpose);
        } catch (Exception e) {
            log.log(Level.WARNING, ">>> RESEND DISPATCH WARNING: " + e.getMessage());
            System.err.println(">>> RESEND WARNING: " + e.getMessage());
            System.out.println(">>> OTP DISPATCH CODE FOR [" + toEmail + "]: [ " + otpCode + " ]");
        }
    }

    public void sendOtpEmail(String to, String otp) {
        sendOtpEmailSync(to, otp, "LOGIN");
    }

    public void sendOtpEmailSync(String toEmail, String otpCode, String purpose) {
        log.info("Resend OTP email request received for: " + toEmail + " (Purpose: " + purpose + ")");

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
        System.out.println(">>> DISPATCHING RESEND API EMAIL TO: [" + toEmail + "] Purpose: " + purpose);
        System.out.println(">>> OTP CODE GENERATED: [ " + otpCode + " ]");
        System.out.println("=================================================");

        try {
            if (apiKey.isEmpty() || apiKey.contains("your_resend_api_key")) {
                throw new IllegalStateException("RESEND_API_KEY is not configured in environment.");
            }

            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("=================================================");
            System.out.println(">>> 200 RESEND API DISPATCH SUCCESSFUL! ID: " + (data != null ? data.getId() : "OK"));
            System.out.println(">>> MAIL SENT TO: [" + toEmail + "] via Resend API");
            System.out.println("=================================================");
            log.info("Resend API email dispatch successful to " + toEmail);
        } catch (Exception e) {
            System.err.println(">>> RESEND DISPATCH NOTICE for [" + toEmail + "]: " + e.getMessage());

            // Resend Testing Mode Fallback: Re-route unverified recipients to account owner email
            if (e.getMessage() != null && (e.getMessage().contains("only send testing emails") || e.getMessage().contains("validation_error") || e.getMessage().contains("403"))) {
                String ownerEmail = "dhanyaande@gmail.com";
                System.out.println(">>> RESEND TESTING MODE: Re-routing email for [" + toEmail + "] to verified account [" + ownerEmail + "]...");
                try {
                    Resend resend = new Resend(apiKey);
                    CreateEmailOptions fallbackParams = CreateEmailOptions.builder()
                            .from(fromAddress)
                            .to(ownerEmail)
                            .subject("[TEST MODE: " + toEmail + "] " + subject)
                            .html(htmlContent)
                            .build();

                    CreateEmailResponse fallbackData = resend.emails().send(fallbackParams);
                    System.out.println("=================================================");
                    System.out.println(">>> 200 RESEND TEST MODE RE-ROUTE SUCCESSFUL!");
                    System.out.println(">>> DELIVERED TO: [" + ownerEmail + "] for user [" + toEmail + "]");
                    System.out.println("=================================================");
                    log.info("Resend API test mode re-routed email to " + ownerEmail);
                    return;
                } catch (Exception fallbackEx) {
                    System.err.println(">>> RESEND RE-ROUTE ERROR: " + fallbackEx.getMessage());
                }
            }

            log.log(Level.SEVERE, "Resend API dispatch failure: " + e.getMessage(), e);
            throw new RuntimeException("Unable to send OTP email via Resend: " + e.getMessage(), e);
        }
    }
}
