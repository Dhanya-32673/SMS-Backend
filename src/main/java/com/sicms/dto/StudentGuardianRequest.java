package com.sicms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class StudentGuardianRequest {

    private String fatherName;
    private String motherName;
    private String guardianName;
    private String relationship;

    @Pattern(regexp = "^$|^[0-9+\\-\\s]{10,20}$", message = "Guardian mobile number must be valid")
    private String guardianMobile;

    @Email(message = "Guardian email must be a valid email address")
    private String guardianEmail;

    private String guardianAddress;

    public StudentGuardianRequest() {
    }

    // Getters and Setters

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

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public void setGuardianEmail(String guardianEmail) {
        this.guardianEmail = guardianEmail;
    }

    public String getGuardianAddress() {
        return guardianAddress;
    }

    public void setGuardianAddress(String guardianAddress) {
        this.guardianAddress = guardianAddress;
    }
}
