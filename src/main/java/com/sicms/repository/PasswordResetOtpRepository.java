package com.sicms.repository;

import com.sicms.entity.PasswordResetOtp;
import com.sicms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);

    Optional<PasswordResetOtp> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant after);

    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true WHERE LOWER(o.email) = LOWER(:email) AND o.used = false")
    void invalidateAllPreviousOtpsForEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.expiresAt < :now OR o.used = true")
    int deleteExpiredOrUsedOtps(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.user = :user")
    void deleteByUser(@Param("user") User user);
}
