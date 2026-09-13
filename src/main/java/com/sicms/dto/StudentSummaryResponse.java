package com.sicms.dto;

import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;

public class StudentSummaryResponse {

    private Long id;
    private String studentId;
    private String admissionNumber;
    private String fullName;
    private String gender;
    private String profilePhotoUrl;
    private StudentStatus status;

    private String branchGroup;
    private String intermediateYear;
    private String section;
    private String campus;
    private String batch;
    private String academicYear;

    public StudentSummaryResponse() {
    }

    public StudentSummaryResponse(Student student) {
        if (student == null) return;
        this.id = student.getId();
        this.studentId = student.getStudentId();
        this.admissionNumber = student.getAdmissionNumber();
        this.fullName = student.getFullName();
        this.gender = student.getGender();
        this.profilePhotoUrl = student.getProfilePhotoUrl();
        this.status = student.getStatus();
        this.branchGroup = com.sicms.util.StudentFormatterUtil.formatBranchGroup(student.getBranchGroup());
        this.intermediateYear = com.sicms.util.StudentFormatterUtil.formatIntermediateYear(student.getIntermediateYear());
        this.section = com.sicms.util.StudentFormatterUtil.formatSection(student.getSection());
        this.campus = student.getCampus();
        this.batch = student.getBatch();
        this.academicYear = student.getAcademicYear();
    }

    // Getters and Setters

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

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
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

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
}
