package com.sicms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_audit_logs", indexes = {
    @Index(name = "idx_export_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_export_audit_created_at", columnList = "created_at")
})
public class ExportAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "section_names", length = 255)
    private String sectionNames;

    @Column(name = "exported_record_count", nullable = false)
    private int exportedRecordCount;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExportAuditLog() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ExportAuditLog(Long userId, String userEmail, String role, String sectionNames, int exportedRecordCount, String ipAddress) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.role = role;
        this.sectionNames = sectionNames;
        this.exportedRecordCount = exportedRecordCount;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSectionNames() { return sectionNames; }
    public void setSectionNames(String sectionNames) { this.sectionNames = sectionNames; }

    public int getExportedRecordCount() { return exportedRecordCount; }
    public void setExportedRecordCount(int exportedRecordCount) { this.exportedRecordCount = exportedRecordCount; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
