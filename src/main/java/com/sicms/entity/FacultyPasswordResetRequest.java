package com.sicms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_password_reset_requests")
public class FacultyPasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(name = "faculty_email", nullable = false, length = 150)
    private String facultyEmail;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @NotBlank
    @Column(name = "otp_hash", nullable = false, length = 128)
    private String otpHash;

    @Column(name = "otp_expiry", nullable = false)
    private LocalDateTime otpExpiry;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_admin", length = 150)
    private String approvedByAdmin;

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
        if (this.used == null) {
            this.used = false;
        }
    }

    public FacultyPasswordResetRequest() {
    }

    public FacultyPasswordResetRequest(String facultyEmail, String employeeId, String reason, String otpHash, LocalDateTime otpExpiry) {
        this.facultyEmail = facultyEmail;
        this.employeeId = employeeId;
        this.reason = reason;
        this.otpHash = otpHash;
        this.otpExpiry = otpExpiry;
        this.used = false;
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFacultyEmail() {
        return facultyEmail;
    }

    public void setFacultyEmail(String facultyEmail) {
        this.facultyEmail = facultyEmail;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedByAdmin() {
        return approvedByAdmin;
    }

    public void setApprovedByAdmin(String approvedByAdmin) {
        this.approvedByAdmin = approvedByAdmin;
    }
}
