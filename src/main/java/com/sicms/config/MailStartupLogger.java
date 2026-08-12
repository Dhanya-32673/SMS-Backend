package com.sicms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MailStartupLogger implements CommandLineRunner {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:dhanyaande@gmail.com}")
    private String mailUsername;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=================================================");
        System.out.println(">>> MAIL CONFIG LOADED SUCCESSFULLY <<<");
        System.out.println(">>> SMTP Host     : " + mailHost);
        System.out.println(">>> SMTP Port     : " + mailPort);
        System.out.println(">>> Sender Account: " + mailUsername);
        System.out.println("=================================================");
    }
}
