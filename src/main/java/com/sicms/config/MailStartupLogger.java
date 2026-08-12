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

    @Value("${resend.api.key:your_resend_api_key_here}")
    private String resendApiKey;

    @Value("${app.mail.from:onboarding@resend.dev}")
    private String mailFrom;

    @Autowired(required = false)
    private EmailService emailService;

    public MailStartupLogger(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> activeProfilesList = Arrays.asList(env.getActiveProfiles());
        String activeProfile = activeProfilesList.toString();

        String trimmedKey = resendApiKey != null ? resendApiKey.trim() : "";
        boolean keyPresent = !trimmedKey.isEmpty() && !trimmedKey.contains("your_resend_api_key");

        System.out.println("=================================================");
        System.out.println(">>> STARTUP DIAGNOSTICS & RESEND MAIL CONFIG");
        System.out.println(">>> Active Profile         : " + activeProfile);
        System.out.println(">>> Datasource URL         : " + dbUrl);
        System.out.println(">>> Datasource Username    : " + dbUsername);
        System.out.println(">>> Sender Address         : " + mailFrom);
        System.out.println(">>> RESEND_API_KEY Present : " + keyPresent);
        System.out.println("=================================================");

        if (!keyPresent) {
            log.warning(">>> RESEND API KEY WARNING: RESEND_API_KEY is missing or invalid. Ensure RESEND_API_KEY is set in Render environment variables.");
        }
    }
}
