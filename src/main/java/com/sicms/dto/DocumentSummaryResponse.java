package com.sicms.dto;

import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentStatus;

import java.time.LocalDateTime;

public class DocumentSummaryResponse {

    private Long id;
    private String studentId;
    private String studentName;
    private String rollNumber;
    private String admissionNumber;
    private String documentTypeName;
    private DocumentCategory category;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private DocumentStatus status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    public DocumentSummaryResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber != null ? rollNumber : admissionNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
        if (this.admissionNumber == null) this.admissionNumber = rollNumber;
    }

    public String getAdmissionNumber() {
        return admissionNumber != null ? admissionNumber : rollNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
        if (this.rollNumber == null) this.rollNumber = admissionNumber;
    }

    public String getDocumentTypeName() {
        return documentTypeName;
    }

    public void setDocumentTypeName(String documentTypeName) {
        this.documentTypeName = documentTypeName;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
