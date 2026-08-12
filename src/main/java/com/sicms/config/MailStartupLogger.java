package com.sicms.config;

import com.sicms.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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

    @Value("${spring.mail.username:b5073a001@smtp-brevo.com}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

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
        String activeProfile = Arrays.toString(env.getActiveProfiles());

        System.out.println("=================================================");
        System.out.println(">>> STARTUP DIAGNOSTICS & MAIL CONFIGURATION");
        System.out.println(">>> Active Profile     : " + activeProfile);
        System.out.println(">>> Datasource URL     : " + dbUrl);
        System.out.println(">>> Datasource Username: " + dbUsername);
        System.out.println(">>> SMTP Host          : " + mailHost);
        System.out.println(">>> SMTP Port          : " + mailPort);
        System.out.println(">>> SMTP Username      : " + mailUsername);
        System.out.println(">>> STARTTLS Enabled   : " + starttlsEnable);
        System.out.println(">>> SSL Enabled        : " + sslEnable);
        System.out.println("=================================================");

        if (mailPassword == null || mailPassword.isBlank() || mailPassword.contains("your_brevo_password_here") || mailPassword.contains("REPLACE_WITH_REAL")) {
            log.warning(">>> BREVO SMTP PASSWORD WARNING: Password is missing or using placeholder text. Set SPRING_MAIL_PASSWORD in environment variables.");
        }

        // Run standalone SMTP test if running with local profile
        if (Arrays.asList(env.getActiveProfiles()).contains("local") && emailService != null) {
            System.out.println(">>> RUNNING LOCAL PROFILE STANDALONE SMTP TEST...");
            emailService.sendStandaloneTestEmail("dhanyaande@gmail.com");
        }
    }
}
