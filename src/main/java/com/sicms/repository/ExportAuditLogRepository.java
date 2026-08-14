package com.sicms.repository;

import com.sicms.entity.ExportAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportAuditLogRepository extends JpaRepository<ExportAuditLog, Long> {
    List<ExportAuditLog> findTop20ByOrderByCreatedAtDesc();

    @Modifying
    @Query(value = "UPDATE export_audit_logs SET user_id = NULL WHERE user_id = :userId", nativeQuery = true)
    void clearUserReferences(@Param("userId") Long userId);
}
