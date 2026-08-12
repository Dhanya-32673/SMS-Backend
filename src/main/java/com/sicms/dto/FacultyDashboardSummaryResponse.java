package com.sicms.dto;

import java.util.List;
import java.util.Map;

public class FacultyDashboardSummaryResponse {

    private long assignedStudentsCount;
    private long totalDocumentsCount;
    private long pendingDocumentsCount;
    private long missingDocuments;
    private double certificateCompletionRate;

    private Map<String, Long> studentsByYear;
    private List<StudentSummaryResponse> assignedStudents;
    private List<StudentSummaryResponse> recentStudents;

    public FacultyDashboardSummaryResponse() {
    }

    public long getAssignedStudentsCount() {
        return assignedStudentsCount;
    }

    public void setAssignedStudentsCount(long assignedStudentsCount) {
        this.assignedStudentsCount = assignedStudentsCount;
    }

    public long getTotalDocumentsCount() {
        return totalDocumentsCount;
    }

    public void setTotalDocumentsCount(long totalDocumentsCount) {
        this.totalDocumentsCount = totalDocumentsCount;
    }

    public long getPendingDocumentsCount() {
        return pendingDocumentsCount;
    }

    public void setPendingDocumentsCount(long pendingDocumentsCount) {
        this.pendingDocumentsCount = pendingDocumentsCount;
    }

    public long getMissingDocuments() {
        return missingDocuments;
    }

    public void setMissingDocuments(long missingDocuments) {
        this.missingDocuments = missingDocuments;
    }

    public double getCertificateCompletionRate() {
        return certificateCompletionRate;
    }

    public void setCertificateCompletionRate(double certificateCompletionRate) {
        this.certificateCompletionRate = certificateCompletionRate;
    }

    public Map<String, Long> getStudentsByYear() {
        return studentsByYear;
    }

    public void setStudentsByYear(Map<String, Long> studentsByYear) {
        this.studentsByYear = studentsByYear;
    }

    public List<StudentSummaryResponse> getAssignedStudents() {
        return assignedStudents;
    }

    public void setAssignedStudents(List<StudentSummaryResponse> assignedStudents) {
        this.assignedStudents = assignedStudents;
    }

    public List<StudentSummaryResponse> getRecentStudents() {
        return recentStudents;
    }

    public void setRecentStudents(List<StudentSummaryResponse> recentStudents) {
        this.recentStudents = recentStudents;
    }
}
