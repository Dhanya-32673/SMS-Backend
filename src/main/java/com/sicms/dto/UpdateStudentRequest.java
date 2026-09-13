package com.sicms.dto;

import com.sicms.entity.StudentStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class UpdateStudentRequest {

    // 2. Admission Number
    private String admissionNumber;

    // 3. Full Name
    @Size(max = 150, message = "Full Name cannot exceed 150 characters")
    private String fullName;

    // 4. Gender
    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    private String gender;

    // 5. Date of Birth
    @Past(message = "Date of Birth must be in the past")
    private LocalDate dateOfBirth;

    // 6. Nationality
    private String nationality;

    // 7. Religion
    private String religion;

    // 8. Category
    private String category;

    // 9. Aadhaar Number
    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar Number must contain exactly 12 digits")
    private String aadhaarNumber;

    // 10. Profile Photo URL
    private String profilePhotoUrl;

    // 11. Mobile Number
    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Mobile Number must contain 10 to 15 digits")
    private String mobileNumber;

    // 12. Alternate Mobile
    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Alternate Mobile must contain 10 to 15 digits")
    private String alternateMobile;

    // 13. Email Address - 1
    @Email(message = "Valid Email Address - 1 is required")
    private String emailAddress1;

    // 14. Email Address - 2
    @Email(message = "Valid Email Address - 2 is required")
    private String emailAddress2;

    // 15. Father Name
    @Size(max = 100, message = "Father Name cannot exceed 100 characters")
    private String fatherName;

    // 16. Mother Name
    @Size(max = 100, message = "Mother Name cannot exceed 100 characters")
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

    // Technical Fields
    private String campus;
    private String section;
    private StudentStatus status;

    public UpdateStudentRequest() {
    }

    // Getters and Setters

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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }
}
