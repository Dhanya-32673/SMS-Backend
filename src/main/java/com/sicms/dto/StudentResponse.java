package com.sicms.dto;

import com.sicms.entity.Student;
import com.sicms.entity.StudentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentResponse {

    // Personal Info
    private Long id;
    private String studentId;
    private String rollNumber;
    private String admissionNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String casteCategory;
    private String aadhaarNumber;
    private String panNumber;
    private String maskedAadhaar;
    private String maskedPan;
    private String motherTongue;
    private String maritalStatus;
    private String identificationMarks;
    private String profilePhotoUrl;
    private StudentStatus status;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Contact Details
    private String mobileNumber;
    private String alternateMobile;
    private String email;
    private String parentEmail;
    private String address;
    private String village;
    private String city;
    private String district;
    private String state;
    private String pinCode;
    private String country;

    // Parent Details
    private String fatherName;
    private String fatherOccupation;
    private String fatherMobile;
    private String fatherEmail;
    private String motherName;
    private String motherOccupation;
    private String motherMobile;
    private String motherEmail;
    private String parentMobile;
    private String occupation;
    private BigDecimal annualIncome;
    private String emergencyContact;

    // Guardian Details
    private String guardianName;
    private String relationship;
    private String guardianMobile;
    private String guardianEmail;
    private String guardianAddress;

    // Academic Details
    private String universityId;
    private String department;
    private String branchGroup;
    private String intermediateYear;
    private Integer semester;
    private String section;
    private Integer sectionCapacity;
    private String batch;
    private String academicYear;
    private LocalDate admissionDate;
    private LocalDate joiningDate;
    private String regulation;
    private String admissionType;
    private String hostelDayScholar;
    private String medium;
    private String previousSchool;
    private String previousBoard;
    private String scholarshipDetails;
    private String busRoute;
    private String assignedFacultyName;

    public StudentResponse() {
    }

    public StudentResponse(Student student) {
        if (student == null) return;

        this.id = student.getId();
        this.studentId = student.getStudentId();
        this.rollNumber = student.getRollNumber();
        this.admissionNumber = student.getAdmissionNumber();
        this.firstName = student.getFirstName();
        this.middleName = student.getMiddleName();
        this.lastName = student.getLastName();
        this.fullName = student.getFullName();
        this.gender = student.getGender();
        this.dateOfBirth = student.getDateOfBirth();
        this.bloodGroup = student.getBloodGroup();
        this.nationality = student.getNationality();
        this.religion = student.getReligion();
        this.casteCategory = student.getCasteCategory();
        this.aadhaarNumber = student.getAadhaarNumber();
        this.panNumber = student.getPanNumber();
        this.maskedAadhaar = maskAadhaar(student.getAadhaarNumber());
        this.maskedPan = maskPan(student.getPanNumber());
        this.identificationMarks = student.getIdentificationMarks();
        this.profilePhotoUrl = student.getProfilePhotoUrl();
        this.status = student.getStatus();
        this.createdByEmail = student.getCreatedBy() != null ? student.getCreatedBy().getEmail() : null;
        this.createdAt = student.getCreatedAt();
        this.updatedAt = student.getUpdatedAt();

        if (student.getContactDetail() != null) {
            this.mobileNumber = student.getContactDetail().getMobileNumber();
            this.alternateMobile = student.getContactDetail().getAlternateMobile();
            this.email = student.getContactDetail().getEmail();
            this.address = student.getContactDetail().getAddress();
            this.city = student.getContactDetail().getCity();
            this.district = student.getContactDetail().getDistrict();
            this.state = student.getContactDetail().getState();
            this.pinCode = student.getContactDetail().getPinCode();
            this.country = student.getContactDetail().getCountry();
        }

        if (student.getParentDetail() != null) {
            this.fatherName = student.getParentDetail().getFatherName();
            this.motherName = student.getParentDetail().getMotherName();
            this.parentMobile = student.getParentDetail().getParentMobile();
            this.parentEmail = student.getParentDetail().getParentEmail();
            this.occupation = student.getParentDetail().getOccupation();
            this.fatherOccupation = student.getParentDetail().getOccupation();
            this.fatherMobile = student.getParentDetail().getParentMobile();
            this.fatherEmail = student.getParentDetail().getParentEmail();
            this.annualIncome = student.getParentDetail().getAnnualIncome();
            this.emergencyContact = student.getParentDetail().getParentMobile();
        }

        if (student.getGuardianDetail() != null) {
            this.guardianName = student.getGuardianDetail().getGuardianName();
            this.relationship = student.getGuardianDetail().getRelationship();
            this.guardianMobile = student.getGuardianDetail().getGuardianMobile();
            this.guardianEmail = student.getGuardianDetail().getGuardianEmail();
            this.guardianAddress = student.getGuardianDetail().getGuardianAddress();
            if (student.getGuardianDetail().getFatherName() != null && this.fatherName == null) {
                this.fatherName = student.getGuardianDetail().getFatherName();
            }
            if (student.getGuardianDetail().getMotherName() != null && this.motherName == null) {
                this.motherName = student.getGuardianDetail().getMotherName();
            }
        }

        if (student.getAcademicDetail() != null) {
            this.universityId = student.getAcademicDetail().getUniversityId();
            this.department = student.getAcademicDetail().getDepartment();
            this.branchGroup = com.sicms.util.StudentFormatterUtil.formatBranchGroup(student.getAcademicDetail().getBranchGroup());
            this.intermediateYear = com.sicms.util.StudentFormatterUtil.formatIntermediateYear(student.getAcademicDetail().getIntermediateYear());
            this.semester = student.getAcademicDetail().getSemester();
            this.section = com.sicms.util.StudentFormatterUtil.formatSection(student.getAcademicDetail().getSection());
            this.batch = student.getAcademicDetail().getBatch();
            this.academicYear = student.getAcademicDetail().getAcademicYear();
            this.admissionDate = student.getAcademicDetail().getAdmissionDate();
            this.joiningDate = student.getAcademicDetail().getAdmissionDate();
            this.regulation = student.getAcademicDetail().getRegulation();
            this.admissionType = student.getAcademicDetail().getAdmissionType();
            this.hostelDayScholar = student.getAcademicDetail().getHostelDayScholar();
            this.medium = student.getAcademicDetail().getMedium();
        } else {
            this.department = "General";
            this.branchGroup = "General";
            this.intermediateYear = "1st Year";
            this.section = "Unassigned";
        }
    }

    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) return null;
        String clean = aadhaar.replaceAll("\\s+", "");
        if (clean.length() >= 4) {
            return "XXXX XXXX " + clean.substring(clean.length() - 4);
        }
        return "XXXX XXXX " + clean;
    }

    public static String maskPan(String pan) {
        if (pan == null || pan.trim().isEmpty()) return null;
        String clean = pan.trim();
        if (clean.length() >= 4) {
            return "XXXXXX" + clean.substring(clean.length() - 4);
        }
        return "XXXXXX" + clean;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getRollNumber() { return rollNumber; }
    public String getAdmissionNumber() { return admissionNumber; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getGender() { return gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getBloodGroup() { return bloodGroup; }
    public String getNationality() { return nationality; }
    public String getReligion() { return religion; }
    public String getCasteCategory() { return casteCategory; }
    public String getAadhaarNumber() { return aadhaarNumber; }
    public String getPanNumber() { return panNumber; }
    public String getMaskedAadhaar() { return maskedAadhaar; }
    public String getMaskedPan() { return maskedPan; }
    public String getMotherTongue() { return motherTongue; }
    public String getMaritalStatus() { return maritalStatus; }
    public String getIdentificationMarks() { return identificationMarks; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public StudentStatus getStatus() { return status; }
    public String getCreatedByEmail() { return createdByEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getMobileNumber() { return mobileNumber; }
    public String getAlternateMobile() { return alternateMobile; }
    public String getEmail() { return email; }
    public String getParentEmail() { return parentEmail; }
    public String getAddress() { return address; }
    public String getVillage() { return village; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getState() { return state; }
    public String getPinCode() { return pinCode; }
    public String getCountry() { return country; }

    public String getFatherName() { return fatherName; }
    public String getFatherOccupation() { return fatherOccupation; }
    public String getFatherMobile() { return fatherMobile; }
    public String getFatherEmail() { return fatherEmail; }
    public String getMotherName() { return motherName; }
    public String getMotherOccupation() { return motherOccupation; }
    public String getMotherMobile() { return motherMobile; }
    public String getMotherEmail() { return motherEmail; }
    public String getParentMobile() { return parentMobile; }
    public String getOccupation() { return occupation; }
    public BigDecimal getAnnualIncome() { return annualIncome; }
    public String getEmergencyContact() { return emergencyContact; }

    public String getGuardianName() { return guardianName; }
    public String getRelationship() { return relationship; }
    public String getGuardianMobile() { return guardianMobile; }
    public String getGuardianEmail() { return guardianEmail; }
    public String getGuardianAddress() { return guardianAddress; }

    public String getUniversityId() { return universityId; }
    public String getDepartment() { return department; }
    public String getBranchGroup() { return branchGroup; }
    public String getIntermediateYear() { return intermediateYear; }
    public Integer getSemester() { return semester; }
    public String getSection() { return section; }
    public Integer getSectionCapacity() { return sectionCapacity; }
    public String getBatch() { return batch; }
    public String getAcademicYear() { return academicYear; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public String getRegulation() { return regulation; }
    public String getAdmissionType() { return admissionType; }
    public String getHostelDayScholar() { return hostelDayScholar; }
    public String getMedium() { return medium; }
    public String getPreviousSchool() { return previousSchool; }
    public String getPreviousBoard() { return previousBoard; }
    public String getScholarshipDetails() { return scholarshipDetails; }
    public String getBusRoute() { return busRoute; }
    public String getAssignedFacultyName() { return assignedFacultyName; }
}
