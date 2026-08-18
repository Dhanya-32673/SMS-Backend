package com.sicms.repository;

import com.sicms.entity.FacultyPasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FacultyPasswordResetRequestRepository extends JpaRepository<FacultyPasswordResetRequest, Long> {

    Optional<FacultyPasswordResetRequest> findFirstByFacultyEmailIgnoreCaseAndUsedFalseOrderByRequestedAtDesc(String facultyEmail);

    long countByFacultyEmailIgnoreCaseAndRequestedAtAfter(String facultyEmail, LocalDateTime afterTime);

    @Modifying
    @Query("UPDATE FacultyPasswordResetRequest r SET r.used = true WHERE LOWER(r.facultyEmail) = LOWER(:email) AND r.used = false")
    void invalidateAllPreviousRequestsForEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM FacultyPasswordResetRequest r WHERE LOWER(r.facultyEmail) = LOWER(:email)")
    void deleteByFacultyEmail(@Param("email") String email);
}
