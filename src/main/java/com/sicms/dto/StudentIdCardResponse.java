package com.sicms.dto;

import com.sicms.entity.Student;

public class StudentIdCardResponse {

    private String collegeLogoUrl = "/logo.png";
    private String collegeName = "COLLEGE OF ARTS, SCIENCE & TECHNOLOGY";
    private String studentPhotoUrl;
    private String studentId;
    private String studentName;
    private String rollNumber;
    private String admissionNumber;
    private String department;
    private String branchGroup;
    private String intermediateYear;
    private String section;
    private String academicYear;
    private String qrCodePayload;

    public StudentIdCardResponse() {
    }

    public StudentIdCardResponse(Student student, String qrCodePayload) {
        this.studentPhotoUrl = student.getProfilePhotoUrl();
        this.studentId = student.getStudentId();
        this.studentName = student.getFullName();
        this.rollNumber = student.getRollNumber();
        this.admissionNumber = student.getAdmissionNumber();

        if (student.getAcademicDetail() != null) {
            this.department = student.getAcademicDetail().getDepartment();
            this.branchGroup = com.sicms.util.StudentFormatterUtil.formatBranchGroup(student.getAcademicDetail().getBranchGroup());
            this.intermediateYear = com.sicms.util.StudentFormatterUtil.formatIntermediateYear(student.getAcademicDetail().getIntermediateYear());
            this.section = com.sicms.util.StudentFormatterUtil.formatSection(student.getAcademicDetail().getSection());
            this.academicYear = student.getAcademicDetail().getAcademicYear();
        } else {
            this.department = "General";
            this.branchGroup = "General";
            this.intermediateYear = "1st Year";
            this.section = "Unassigned";
        }

        this.qrCodePayload = qrCodePayload;
    }

    // Getters and Setters

    public String getCollegeLogoUrl() {
        return collegeLogoUrl;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public String getStudentPhotoUrl() {
        return studentPhotoUrl;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getBranchGroup() {
        return branchGroup;
    }

    public String getIntermediateYear() {
        return intermediateYear;
    }

    public String getSection() {
        return section;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getQrCodePayload() {
        return qrCodePayload;
    }
}
