package com.sicms.dto;

import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;

public class StudentSearchResponse {

    private String studentId;
    private String rollNumber;
    private String admissionNumber;
    private String fullName;
    private String gender;
    private String profilePhotoUrl;
    private StudentStatus status;

    private String department;
    private String branchGroup;
    private String intermediateYear;
    private String section;

    public StudentSearchResponse() {
    }

    public StudentSearchResponse(Student student) {
        this.studentId = student.getStudentId();
        this.rollNumber = student.getRollNumber();
        this.admissionNumber = student.getAdmissionNumber();
        this.fullName = student.getFullName();
        this.gender = student.getGender();
        this.profilePhotoUrl = student.getProfilePhotoUrl();
        this.status = student.getStatus();

        if (student.getAcademicDetail() != null) {
            this.department = student.getAcademicDetail().getDepartment();
            this.branchGroup = com.sicms.util.StudentFormatterUtil.formatBranchGroup(student.getAcademicDetail().getBranchGroup());
            this.intermediateYear = com.sicms.util.StudentFormatterUtil.formatIntermediateYear(student.getAcademicDetail().getIntermediateYear());
            this.section = com.sicms.util.StudentFormatterUtil.formatSection(student.getAcademicDetail().getSection());
        } else {
            this.department = "General";
            this.branchGroup = "General";
            this.intermediateYear = "1st Year";
            this.section = "Unassigned";
        }
    }

    // Getters and Setters

    public String getStudentId() {
        return studentId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public StudentStatus getStatus() {
        return status;
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
}
