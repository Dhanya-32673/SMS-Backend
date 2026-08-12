package com.sicms.dto;

import java.util.List;
import java.util.Map;

public class AdminDashboardSummaryResponse {

    private long totalStudents;
    private long totalFaculty;
    private long newAdmissions;

    private long totalCertificates;
    private long uploadedCertificates;
    private long missingDocuments;
    private long pendingVerification;
    private long verifiedDocuments;
    private long rejectedDocuments;
    private double certificateCompletionRate;

    private Map<String, Long> studentsByDepartment;
    private Map<String, Long> certificateStatusDistribution;
    private List<Map<String, Object>> monthlyRegistrations;
    private List<StudentSummaryResponse> recentStudents;

    public AdminDashboardSummaryResponse() {
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalFaculty() {
        return totalFaculty;
    }

    public void setTotalFaculty(long totalFaculty) {
        this.totalFaculty = totalFaculty;
    }

    public long getNewAdmissions() {
        return newAdmissions;
    }

    public void setNewAdmissions(long newAdmissions) {
        this.newAdmissions = newAdmissions;
    }

    public long getTotalCertificates() {
        return totalCertificates;
    }

    public void setTotalCertificates(long totalCertificates) {
        this.totalCertificates = totalCertificates;
    }

    public long getUploadedCertificates() {
        return uploadedCertificates;
    }

    public void setUploadedCertificates(long uploadedCertificates) {
        this.uploadedCertificates = uploadedCertificates;
    }

    public long getMissingDocuments() {
        return missingDocuments;
    }

    public void setMissingDocuments(long missingDocuments) {
        this.missingDocuments = missingDocuments;
    }

    public long getPendingVerification() {
        return pendingVerification;
    }

    public void setPendingVerification(long pendingVerification) {
        this.pendingVerification = pendingVerification;
    }

    public long getVerifiedDocuments() {
        return verifiedDocuments;
    }

    public void setVerifiedDocuments(long verifiedDocuments) {
        this.verifiedDocuments = verifiedDocuments;
    }

    public long getRejectedDocuments() {
        return rejectedDocuments;
    }

    public void setRejectedDocuments(long rejectedDocuments) {
        this.rejectedDocuments = rejectedDocuments;
    }

    public double getCertificateCompletionRate() {
        return certificateCompletionRate;
    }

    public void setCertificateCompletionRate(double certificateCompletionRate) {
        this.certificateCompletionRate = certificateCompletionRate;
    }

    public Map<String, Long> getStudentsByDepartment() {
        return studentsByDepartment;
    }

    public void setStudentsByDepartment(Map<String, Long> studentsByDepartment) {
        this.studentsByDepartment = studentsByDepartment;
    }

    public Map<String, Long> getCertificateStatusDistribution() {
        return certificateStatusDistribution;
    }

    public void setCertificateStatusDistribution(Map<String, Long> certificateStatusDistribution) {
        this.certificateStatusDistribution = certificateStatusDistribution;
    }

    public List<Map<String, Object>> getMonthlyRegistrations() {
        return monthlyRegistrations;
    }

    public void setMonthlyRegistrations(List<Map<String, Object>> monthlyRegistrations) {
        this.monthlyRegistrations = monthlyRegistrations;
    }

    public List<StudentSummaryResponse> getRecentStudents() {
        return recentStudents;
    }

    public void setRecentStudents(List<StudentSummaryResponse> recentStudents) {
        this.recentStudents = recentStudents;
    }
}
