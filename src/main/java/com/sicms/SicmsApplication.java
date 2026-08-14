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

    static {
        loadDotEnv();
    }

    private static void loadDotEnv() {
        try {
            java.io.File[] possiblePaths = new java.io.File[] {
                new java.io.File(".env"),
                new java.io.File("Backend/.env"),
                new java.io.File("../Backend/.env"),
                new java.io.File("../../Backend/.env")
            };

            for (java.io.File envFile : possiblePaths) {
                if (envFile.exists() && envFile.isFile()) {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    System.out.println("✓ Loaded environment properties from: " + envFile.getCanonicalPath());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load .env file: " + e.getMessage());
        }
    }

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
