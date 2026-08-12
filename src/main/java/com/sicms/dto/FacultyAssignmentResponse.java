package com.sicms.dto;

import java.time.OffsetDateTime;

public class FacultyAssignmentResponse {

    private Long id;
    private Long facultyId;
    private String facultyName;
    private String branchGroup;
    private String intermediateYear;
    private String section;
    private String academicYear;
    private String subjectName;
    private boolean active;
    private OffsetDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
