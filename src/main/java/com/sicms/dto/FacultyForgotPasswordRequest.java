package com.sicms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class FacultyForgotPasswordRequest {

    @NotBlank(message = "Faculty email is required")
    @Email(message = "Invalid email format")
    private String facultyEmail;

    private String employeeId;

    private String reason;

    public FacultyForgotPasswordRequest() {
    }

    public FacultyForgotPasswordRequest(String facultyEmail, String employeeId, String reason) {
        this.facultyEmail = facultyEmail;
        this.employeeId = employeeId;
        this.reason = reason;
    }

    public String getFacultyEmail() {
        return facultyEmail;
    }

    public void setFacultyEmail(String facultyEmail) {
        this.facultyEmail = facultyEmail;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
