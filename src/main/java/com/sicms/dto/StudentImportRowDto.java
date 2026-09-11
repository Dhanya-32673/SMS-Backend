package com.sicms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentImportRowDto {

    private int rowNumber;
    private String status = "VALID"; // VALID, WARNING, ERROR, DUPLICATE
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    // 1. Identifiers & Personal Details
    private String studentId;
    private String admissionNumber;
    private String rollNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String nationality = "Indian";
    private String religion;
    private String casteCategory;
    private String aadhaarNumber;
    private String panNumber;
    private String identificationMarks;
    private String profilePhotoUrl;
    private String studentStatus = "ACTIVE";

    // 2. Contact Information
    private String mobileNumber;
    private String alternateMobile;
    private String email;
    private String address;
    private String city = "Hyderabad";
    private String district = "Hyderabad";
    private String state = "Telangana";
    private String pinCode = "500001";
    private String country = "India";

    // 3. Parent / Guardian Information
    private String fatherName;
    private String motherName;
    private String parentMobile;
    private String parentEmail;
    private String occupation;
    private BigDecimal annualIncome;
    private String guardianName;
    private String guardianMobile;
    private String guardianRelation;
    private String guardianAddress;

    // 4. Academic Information
    private String academicYear;
    private String department = "General";
    private String branchGroup;
    private String intermediateYear;
    private Integer semester;
    private String section;
    private String batch;
    private LocalDate admissionDate;
    private String admissionType = "REGULAR";
    private String hostelDayScholar = "DAY_SCHOLAR";
    private String medium = "English";
    private String regulation;
    private String universityId;

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

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName.trim());
        if (middleName != null && !middleName.trim().isEmpty()) sb.append(" ").append(middleName.trim());
        if (lastName != null) sb.append(" ").append(lastName.trim());
        return sb.toString();
    }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCasteCategory() { return casteCategory; }
    public void setCasteCategory(String casteCategory) { this.casteCategory = casteCategory; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getIdentificationMarks() { return identificationMarks; }
    public void setIdentificationMarks(String identificationMarks) { this.identificationMarks = identificationMarks; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getAlternateMobile() { return alternateMobile; }
    public void setAlternateMobile(String alternateMobile) { this.alternateMobile = alternateMobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getParentMobile() { return parentMobile; }
    public void setParentMobile(String parentMobile) { this.parentMobile = parentMobile; }

    public String getParentEmail() { return parentEmail; }
    public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianMobile() { return guardianMobile; }
    public void setGuardianMobile(String guardianMobile) { this.guardianMobile = guardianMobile; }

    public String getGuardianRelation() { return guardianRelation; }
    public void setGuardianRelation(String guardianRelation) { this.guardianRelation = guardianRelation; }

    public String getGuardianAddress() { return guardianAddress; }
    public void setGuardianAddress(String guardianAddress) { this.guardianAddress = guardianAddress; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public String getHostelDayScholar() { return hostelDayScholar; }
    public void setHostelDayScholar(String hostelDayScholar) { this.hostelDayScholar = hostelDayScholar; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public String getRegulation() { return regulation; }
    public void setRegulation(String regulation) { this.regulation = regulation; }

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }
}
