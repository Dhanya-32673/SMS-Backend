package com.sicms.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentImportRowDto {

    private int rowNumber;
    private String status = "VALID"; // VALID, WARNING, ERROR, DUPLICATE
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

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
    private String nationality = "Indian";

    // 7. Religion
    private String religion;

    // 8. Category
    private String category;

    // 9. Aadhaar Number
    private String aadhaarNumber;

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
    private String admissionType = "REGULAR";

    // 22. Hostel / Day Scholar
    private String hostelDayScholar = "DAY_SCHOLAR";

    // 23. Campus
    private String campus;

    // Optional Internal Technical Assignment
    private String section = "Unassigned";

    public StudentImportRowDto() {
    }

    public void addError(String error) {
        this.errors.add(error);
        this.status = "ERROR";
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
        if (!"ERROR".equals(this.status)) {
            this.status = "WARNING";
        }
    }

    public void markDuplicate(String message) {
        this.errors.add(message);
        this.status = "DUPLICATE";
    }

    public boolean isValid() {
        return "VALID".equals(this.status) || "WARNING".equals(this.status);
    }

    // Getters and Setters

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getAlternateMobile() { return alternateMobile; }
    public void setAlternateMobile(String alternateMobile) { this.alternateMobile = alternateMobile; }

    public String getEmailAddress1() { return emailAddress1; }
    public void setEmailAddress1(String emailAddress1) { this.emailAddress1 = emailAddress1; }

    public String getEmailAddress2() { return emailAddress2; }
    public void setEmailAddress2(String emailAddress2) { this.emailAddress2 = emailAddress2; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public String getHostelDayScholar() { return hostelDayScholar; }
    public void setHostelDayScholar(String hostelDayScholar) { this.hostelDayScholar = hostelDayScholar; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
