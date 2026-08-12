package com.sicms.config;

import com.sicms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseStartupLogger implements CommandLineRunner {

    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final Environment environment;

    @Value("${spring.datasource.url:N/A}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:N/A}")
    private String datasourceUsername;

    public DatabaseStartupLogger(DataSource dataSource, UserRepository userRepository, Environment environment) {
        this.dataSource = dataSource;
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================================");
        System.out.println(">>> STARTUP DIAGNOSTICS & SYSTEM INITIALIZATION");

        String activeProfiles = String.join(", ", environment.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default (prod)";
        }
        System.out.println(">>> Active Profile         : [" + activeProfiles + "]");
        
        // Mask password in datasource URL if present
        String maskedUrl = datasourceUrl.replaceAll(":[^/@]+@", ":****@");
        System.out.println(">>> Datasource URL         : [" + maskedUrl + "]");
        System.out.println(">>> Datasource Username    : [" + datasourceUsername + "]");

        boolean dbReachable = false;
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                dbReachable = true;
                System.out.println(">>> Database Connection    : [SUCCESS] - Connection established successfully!");
            }
        } catch (Exception e) {
            System.err.println(">>> Database Connection    : [FAILED] - Reason: " + e.getMessage());
        }

        try {
            long userCount = userRepository.count();
            System.out.println(">>> Hibernate Initialized  : [SUCCESS] - EntityManagerFactory loaded.");
            System.out.println(">>> JPA Repositories Loaded: [SUCCESS] - UserRepository active (Registered Users: " + userCount + ")");
        } catch (Exception e) {
            System.err.println(">>> JPA Repositories Loaded: [FAILED] - Reason: " + e.getMessage());
        }

        if (dbReachable) {
            System.out.println(">>> SYSTEM STATUS          : [ONLINE & READY FOR DEPLOYMENT]");
        } else {
            System.err.println(">>> SYSTEM STATUS          : [WARNING - DATABASE DISCONNECTED]");
        }
        System.out.println("=================================================");
    }
}
