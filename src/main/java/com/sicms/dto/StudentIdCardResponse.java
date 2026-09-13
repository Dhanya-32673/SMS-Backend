package com.sicms.dto;

import com.sicms.entity.Student;

public class StudentIdCardResponse {

    private String collegeLogoUrl = "/logo.png";
    private String collegeName = "COLLEGE OF ARTS, SCIENCE & TECHNOLOGY";
    private String studentPhotoUrl;
    private String studentId;
    private String studentName;
    private String admissionNumber;
    private String branchGroup;
    private String intermediateYear;
    private String section;
    private String academicYear;
    private String qrCodePayload;

    public StudentIdCardResponse() {
    }

    public StudentIdCardResponse(Student student, String qrCodePayload) {
        if (student == null) return;
        this.studentPhotoUrl = student.getProfilePhotoUrl();
        this.studentId = student.getStudentId();
        this.studentName = student.getFullName();
        this.admissionNumber = student.getAdmissionNumber();
        this.branchGroup = com.sicms.util.StudentFormatterUtil.formatBranchGroup(student.getBranchGroup());
        this.intermediateYear = com.sicms.util.StudentFormatterUtil.formatIntermediateYear(student.getIntermediateYear());
        this.section = com.sicms.util.StudentFormatterUtil.formatSection(student.getSection());
        this.academicYear = student.getAcademicYear();
        this.qrCodePayload = qrCodePayload;
    }

    // Getters and Setters

    public String getCollegeLogoUrl() {
        return collegeLogoUrl;
    }

    public void setCollegeLogoUrl(String collegeLogoUrl) {
        this.collegeLogoUrl = collegeLogoUrl;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getStudentPhotoUrl() {
        return studentPhotoUrl;
    }

    public void setStudentPhotoUrl(String studentPhotoUrl) {
        this.studentPhotoUrl = studentPhotoUrl;
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

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getBranchGroup() {
        return branchGroup;
    }

    public void setBranchGroup(String branchGroup) {
        this.branchGroup = branchGroup;
    }

    public String getIntermediateYear() {
        return intermediateYear;
    }

    public void setIntermediateYear(String intermediateYear) {
        this.intermediateYear = intermediateYear;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getQrCodePayload() {
        return qrCodePayload;
    }

    public void setQrCodePayload(String qrCodePayload) {
        this.qrCodePayload = qrCodePayload;
    }
}
