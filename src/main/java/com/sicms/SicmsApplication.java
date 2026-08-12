package com.sicms;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class SicmsApplication {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private String mailPort;

    @PostConstruct
    public void logMailConfig() {
        System.out.println("MAIL HOST: " + mailHost);
        System.out.println("MAIL PORT: " + mailPort);
    }

    public static void main(String[] args) {
        SpringApplication.run(SicmsApplication.class, args);
    }

}
