package com.sicms.dto;

import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentResponse {

    // Technical Fields
    private Long id;
    private StudentStatus status;
    private String section;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 1. Student ID
    private String studentId;

    // 2. Admission Number
    private String admissionNumber;

    // 3. Full Name
    private String fullName;

    // 4. Gender
    private String gender;

    // 5. Date of Birth
    private LocalDate dateOfBirth;

    // 6. Nationality
    private String nationality;

    // 7. Religion
    private String religion;

    // 8. Category
    private String category;

    // 9. Aadhaar Number & Masked Aadhaar
    private String aadhaarNumber;
    private String maskedAadhaar;

    // 10. Profile Photo URL
    private String profilePhotoUrl;

    // 11. Mobile Number
    private String mobileNumber;

    // 12. Alternate Mobile
    private String alternateMobile;

    // 13. Email Address - 1
    private String emailAddress1;

    // 14. Email Address - 2
    private String emailAddress2;

    // 15. Father Name
    private String fatherName;

    // 16. Mother Name
    private String motherName;

    // 17. Academic Year
    private String academicYear;

    // 18. Branch / Group
    private String branchGroup;

    // 19. Intermediate Year
    private String intermediateYear;

    // 20. Batch
    private String batch;

    // 21. Admission Type
    private String admissionType;

    // 22. Hostel / Day Scholar
    private String hostelDayScholar;

    // 23. Campus
    private String campus;
    private Long campusId;

    public StudentResponse() {
    }

    public StudentResponse(Student student) {
        if (student == null) return;

        this.id = student.getId();
        this.studentId = student.getStudentId();
        this.admissionNumber = student.getAdmissionNumber();
        this.fullName = student.getFullName();
        this.gender = student.getGender();
        this.dateOfBirth = student.getDateOfBirth();
        this.nationality = student.getNationality();
        this.religion = student.getReligion();
        this.category = student.getCategory();
        this.aadhaarNumber = student.getAadhaarNumber();
        this.maskedAadhaar = maskAadhaar(student.getAadhaarNumber());
        this.profilePhotoUrl = student.getProfilePhotoUrl();
        this.mobileNumber = student.getMobileNumber();
        this.alternateMobile = student.getAlternateMobile();
        this.emailAddress1 = student.getEmailAddress1();
        this.emailAddress2 = student.getEmailAddress2();
        this.fatherName = student.getFatherName();
        this.motherName = student.getMotherName();
        this.academicYear = student.getAcademicYear();
        this.branchGroup = student.getBranchGroup();
        this.intermediateYear = student.getIntermediateYear();
        this.batch = student.getBatch();
        this.admissionType = student.getAdmissionType();
        this.hostelDayScholar = student.getHostelDayScholar();
        this.campus = student.getCampus();
        this.campusId = student.getCampusId();

        this.section = student.getSection() != null ? student.getSection() : "Unassigned";
        this.status = student.getStatus();
        this.createdByEmail = student.getCreatedBy() != null ? student.getCreatedBy().getEmail() : null;
        this.createdAt = student.getCreatedAt();
        this.updatedAt = student.getUpdatedAt();
    }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "Not Provided";
        String clean = aadhaar.trim();
        if (clean.length() == 12) {
            return "XXXX-XXXX-" + clean.substring(8);
        }
        return "XXXX-" + clean.substring(Math.max(0, clean.length() - 4));
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getMaskedAadhaar() {
        return maskedAadhaar;
    }

    public void setMaskedAadhaar(String maskedAadhaar) {
        this.maskedAadhaar = maskedAadhaar;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAlternateMobile() {
        return alternateMobile;
    }

    public void setAlternateMobile(String alternateMobile) {
        this.alternateMobile = alternateMobile;
    }

    public String getEmailAddress1() {
        return emailAddress1;
    }

    public void setEmailAddress1(String emailAddress1) {
        this.emailAddress1 = emailAddress1;
    }

    public String getEmailAddress2() {
        return emailAddress2;
    }

    public void setEmailAddress2(String emailAddress2) {
        this.emailAddress2 = emailAddress2;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
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

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(String admissionType) {
        this.admissionType = admissionType;
    }

    public String getHostelDayScholar() {
        return hostelDayScholar;
    }

    public void setHostelDayScholar(String hostelDayScholar) {
        this.hostelDayScholar = hostelDayScholar;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public Long getCampusId() {
        return campusId;
    }

    public void setCampusId(Long campusId) {
        this.campusId = campusId;
    }
}
