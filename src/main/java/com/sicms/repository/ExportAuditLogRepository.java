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
    @Query("UPDATE ExportAuditLog e SET e.user = null WHERE e.user.id = :userId")
    void clearUserReferences(@Param("userId") Long userId);
}
