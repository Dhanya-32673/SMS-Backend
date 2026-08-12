package com.sicms.config;

import com.sicms.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OtpCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpCleanupScheduler.class);
    private final OtpRepository otpRepository;

    public OtpCleanupScheduler(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /**
     * Purge expired or used OTPs every hour to prevent table bloat
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            int deletedCount = otpRepository.deleteExpiredOrUsedOtps(Instant.now());
            if (deletedCount > 0) {
                log.info("[OTP CLEANUP] Purged {} expired/used OTP records from database.", deletedCount);
            }
        } catch (Exception e) {
            log.error("[OTP CLEANUP] Scheduled OTP cleanup failed: {}", e.getMessage(), e);
        }
    }
}
