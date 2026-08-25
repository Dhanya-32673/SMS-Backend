package com.sicms.dto;

import java.util.List;

public class StudentMissingCertificateDto {

    private Long id;
    private String studentId;
    private String fullName;
    private String rollNumber;
    private String admissionNumber;
    private String branchGroup;
    private String intermediateYear;
    private String academicYear;
    private String section;
    private String profilePhotoUrl;
    private String status;

    private int totalRequiredCount;
    private int uploadedCount;
    private int missingCount;
    private double completionPercentage;

    private List<DocumentTypeItem> missingCertificates;
    private List<DocumentTypeItem> uploadedCertificates;

    public StudentMissingCertificateDto() {
    }

    public static class DocumentTypeItem {
        private Long id;
        private String code;
        private String name;
        private String category;
        private String status;

        public DocumentTypeItem() {
        }

        public DocumentTypeItem(Long id, String code, String name, String category, String status) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.category = category;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }

    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalRequiredCount() { return totalRequiredCount; }
    public void setTotalRequiredCount(int totalRequiredCount) { this.totalRequiredCount = totalRequiredCount; }

    public int getUploadedCount() { return uploadedCount; }
    public void setUploadedCount(int uploadedCount) { this.uploadedCount = uploadedCount; }

    public int getMissingCount() { return missingCount; }
    public void setMissingCount(int missingCount) { this.missingCount = missingCount; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public List<DocumentTypeItem> getMissingCertificates() { return missingCertificates; }
    public void setMissingCertificates(List<DocumentTypeItem> missingCertificates) { this.missingCertificates = missingCertificates; }

    public List<DocumentTypeItem> getUploadedCertificates() { return uploadedCertificates; }
    public void setUploadedCertificates(List<DocumentTypeItem> uploadedCertificates) { this.uploadedCertificates = uploadedCertificates; }
}
