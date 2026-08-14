package com.sicms.repository;

import com.sicms.entity.ExportAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportAuditLogRepository extends JpaRepository<ExportAuditLog, Long> {
    List<ExportAuditLog> findTop20ByOrderByCreatedAtDesc();
}
