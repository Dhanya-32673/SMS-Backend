package com.sicms.dto;

import jakarta.validation.constraints.NotBlank;

public class FacultyAssignmentRequest {

    @NotBlank(message = "Branch Group is required")
    private String branchGroup;

    @NotBlank(message = "Intermediate Year is required")
    private String intermediateYear;

    @NotBlank(message = "Section is required")
    private String section;

    @NotBlank(message = "Academic Year is required")
    private String academicYear;

    private String subjectName;

    // Getters and Setters
    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
}
