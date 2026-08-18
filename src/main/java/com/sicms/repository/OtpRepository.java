package com.sicms.repository;

import com.sicms.entity.OtpPurpose;
import com.sicms.entity.OtpVerification;
import com.sicms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(User user, OtpPurpose purpose);

    Optional<OtpVerification> findTopByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant after);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.used = true WHERE o.user = :user AND o.purpose = :purpose AND o.used = false")
    void invalidateAllPreviousOtpsForUser(@Param("user") User user, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.used = true WHERE LOWER(o.email) = LOWER(:email) AND o.purpose = :purpose AND o.used = false")
    void invalidateAllPreviousOtpsForEmail(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now OR o.used = true")
    int deleteExpiredOrUsedOtps(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.user = :user")
    void deleteByUser(@Param("user") User user);
}
