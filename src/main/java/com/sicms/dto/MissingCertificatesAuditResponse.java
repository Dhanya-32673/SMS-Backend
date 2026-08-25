package com.sicms.dto;

import java.util.List;

public class MissingCertificatesAuditResponse {

    private int totalActiveStudents;
    private int compliantStudentsCount;
    private int missingStudentsCount;
    private double compliancePercentage;

    private List<StudentMissingCertificateDto.DocumentTypeItem> mandatoryDocumentTypes;
    private List<StudentMissingCertificateDto> studentsWithMissing;

    public MissingCertificatesAuditResponse() {
    }

    public MissingCertificatesAuditResponse(
            int totalActiveStudents,
            int compliantStudentsCount,
            int missingStudentsCount,
            double compliancePercentage,
            List<StudentMissingCertificateDto.DocumentTypeItem> mandatoryDocumentTypes,
            List<StudentMissingCertificateDto> studentsWithMissing
    ) {
        this.totalActiveStudents = totalActiveStudents;
        this.compliantStudentsCount = compliantStudentsCount;
        this.missingStudentsCount = missingStudentsCount;
        this.compliancePercentage = compliancePercentage;
        this.mandatoryDocumentTypes = mandatoryDocumentTypes;
        this.studentsWithMissing = studentsWithMissing;
    }

    public int getTotalActiveStudents() {
        return totalActiveStudents;
    }

    public void setTotalActiveStudents(int totalActiveStudents) {
        this.totalActiveStudents = totalActiveStudents;
    }

    public int getCompliantStudentsCount() {
        return compliantStudentsCount;
    }

    public void setCompliantStudentsCount(int compliantStudentsCount) {
        this.compliantStudentsCount = compliantStudentsCount;
    }

    public int getMissingStudentsCount() {
        return missingStudentsCount;
    }

    public void setMissingStudentsCount(int missingStudentsCount) {
        this.missingStudentsCount = missingStudentsCount;
    }

    public double getCompliancePercentage() {
        return compliancePercentage;
    }

    public void setCompliancePercentage(double compliancePercentage) {
        this.compliancePercentage = compliancePercentage;
    }

    public List<StudentMissingCertificateDto.DocumentTypeItem> getMandatoryDocumentTypes() {
        return mandatoryDocumentTypes;
    }

    public void setMandatoryDocumentTypes(List<StudentMissingCertificateDto.DocumentTypeItem> mandatoryDocumentTypes) {
        this.mandatoryDocumentTypes = mandatoryDocumentTypes;
    }

    public List<StudentMissingCertificateDto> getStudentsWithMissing() {
        return studentsWithMissing;
    }

    public void setStudentsWithMissing(List<StudentMissingCertificateDto> studentsWithMissing) {
        this.studentsWithMissing = studentsWithMissing;
    }
}
