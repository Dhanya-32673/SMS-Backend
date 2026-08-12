package com.sicms.config;

import com.sicms.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Component
public class MailStartupLogger implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(MailStartupLogger.class.getName());

    private final Environment env;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/sicms}")
    private String dbUrl;

    @Value("${spring.datasource.username:postgres}")
    private String dbUsername;

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:bhashyamgnt.edu@gmail.com}")
    private String mailFrom;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean sslEnable;

    @Autowired(required = false)
    private EmailService emailService;

    public MailStartupLogger(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> activeProfilesList = Arrays.asList(env.getActiveProfiles());
        String activeProfile = activeProfilesList.toString();

        String trimmedUsername = mailUsername != null ? mailUsername.trim() : "";
        String trimmedPassword = mailPassword != null ? mailPassword.trim() : "";

        boolean usernamePresent = !trimmedUsername.isEmpty();
        boolean passwordPresent = !trimmedPassword.isEmpty();

        boolean isPlaceholderPassword = trimmedPassword.contains("your_brevo_password_here") ||
                trimmedPassword.contains("CHANGE_ME") ||
                trimmedPassword.equalsIgnoreCase("PASSWORD") ||
                trimmedPassword.contains("example") ||
                trimmedPassword.contains("REPLACE_WITH_REAL") ||
                trimmedPassword.contains("PASTE_REAL_BREVO_SMTP_KEY");

        System.out.println("=================================================");
        System.out.println(">>> STARTUP DIAGNOSTICS & MAIL CONFIGURATION");
        System.out.println(">>> Active Profile         : " + activeProfile);
        System.out.println(">>> Datasource URL         : " + dbUrl);
        System.out.println(">>> Datasource Username    : " + dbUsername);
        System.out.println(">>> SMTP Host              : " + mailHost);
        System.out.println(">>> SMTP Port              : " + mailPort);
        System.out.println(">>> SMTP Username          : " + mailUsername);
        System.out.println(">>> Sender Address         : " + mailFrom);
        System.out.println(">>> STARTTLS Enabled       : " + starttlsEnable);
        System.out.println(">>> SSL Enabled            : " + sslEnable);
        System.out.println(">>> SPRING_MAIL_USERNAME present: " + usernamePresent);
        System.out.println(">>> SPRING_MAIL_PASSWORD present: " + (passwordPresent && !isPlaceholderPassword));
        System.out.println("=================================================");

        // Verify Brevo Sender Identity
        if (!"bhashyamgnt.edu@gmail.com".equalsIgnoreCase(mailFrom.trim())) {
            log.warning(">>> BREVO SENDER WARNING: app.mail.from (" + mailFrom + ") is not the primary verified Brevo sender identity. Expected: bhashyamgnt.edu@gmail.com");
        }

        // Fail-Fast Validation in Production
        if (activeProfilesList.contains("prod")) {
            if (!passwordPresent || isPlaceholderPassword) {
                throw new IllegalStateException("FATAL: SPRING_MAIL_PASSWORD is missing or set to placeholder in Render Environment Variables");
            }
        } else {
            if (!passwordPresent || isPlaceholderPassword) {
                log.warning(">>> BREVO SMTP PASSWORD WARNING: Password is missing or set to placeholder text in local environment.");
            } else if (activeProfilesList.contains("local") && emailService != null) {
                System.out.println(">>> RUNNING LOCAL PROFILE STANDALONE SMTP TEST...");
                emailService.sendStandaloneTestEmail("dhanyaande@gmail.com");
            }
        }
    }
}
