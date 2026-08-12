package com.sicms.dto;

import com.sicms.entity.StudentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateStudentRequest {

    @NotBlank(message = "Roll number is required")
    @Size(max = 50, message = "Roll number cannot exceed 50 characters")
    private String rollNumber;

    private String admissionNumber;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    private String fullName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String bloodGroup;
    private String nationality = "Indian";
    private String religion;
    private String casteCategory;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be 12 digits")
    private String aadhaarNumber; // Optional & Sensitive

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number format must be valid (e.g. ABCDE1234F)")
    private String panNumber; // Optional & Sensitive

    private String identificationMarks;
    private String profilePhotoUrl;
    private StudentStatus status = StudentStatus.ACTIVE;

    // Contact Information
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
    private String alternateMobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    private String city = "Hyderabad";
    private String district = "Hyderabad";
    private String state = "Telangana";
    private String pinCode = "500001";
    private String country = "India";

    // Parent Information
    @NotBlank(message = "Father name is required")
    private String fatherName;

    @NotBlank(message = "Mother name is required")
    private String motherName;

    @NotBlank(message = "Parent mobile is required")
    private String parentMobile;

    private String parentEmail;
    private String occupation;

    @PositiveOrZero(message = "Annual income cannot be negative")
    private BigDecimal annualIncome;

    // Academic Information
    private String universityId;
    private String department = "General";

    @NotBlank(message = "Branch/Group is required (e.g. MPC, BiPC, MEC, CEC, HEC)")
    private String branchGroup;

    @NotBlank(message = "Intermediate Year is required (1st Year / 2nd Year)")
    private String intermediateYear;

    private Integer semester;

    @NotBlank(message = "Section is required")
    private String section;

    @NotBlank(message = "Batch is required (e.g. 2026-2028)")
    private String batch;

    @NotBlank(message = "Academic year is required (e.g. 2026-2027)")
    private String academicYear;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    private String regulation;
    private String admissionType = "REGULAR";

    @NotBlank(message = "Hostel/Day Scholar selection is required")
    private String hostelDayScholar = "DAY_SCHOLAR";

    private String medium = "English";

    public CreateStudentRequest() {
    }

    // Getters and Setters

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName.trim());
        if (middleName != null && !middleName.trim().isEmpty()) sb.append(" ").append(middleName.trim());
        if (lastName != null) sb.append(" ").append(lastName.trim());
        return sb.toString();
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

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
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

    public String getCasteCategory() {
        return casteCategory;
    }

    public void setCasteCategory(String casteCategory) {
        this.casteCategory = casteCategory;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getIdentificationMarks() {
        return identificationMarks;
    }

    public void setIdentificationMarks(String identificationMarks) {
        this.identificationMarks = identificationMarks;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getParentMobile() {
        return parentMobile;
    }

    public void setParentMobile(String parentMobile) {
        this.parentMobile = parentMobile;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public String getUniversityId() {
        return universityId;
    }

    public void setUniversityId(String universityId) {
        this.universityId = universityId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
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

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getRegulation() {
        return regulation;
    }

    public void setRegulation(String regulation) {
        this.regulation = regulation;
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

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }
}
