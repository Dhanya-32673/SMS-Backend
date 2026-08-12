package com.sicms.dto;

import java.time.OffsetDateTime;

public class SectionResponse {

    private Long id;
    private String name;
    private String branchGroup;
    private String intermediateYear;
    private String academicYear;
    private Integer capacity;
    private String description;
    private boolean active;
    private long totalStudents;
    private String assignedFacultyName;
    private Long assignedFacultyId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSectionId() { return id; }
    public void setSectionId(Long sectionId) { this.id = sectionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSectionName() { return name; }
    public void setSectionName(String sectionName) { this.name = sectionName; }

    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getGroup() { return branchGroup; }
    public void setGroup(String group) { this.branchGroup = group; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public String getYear() { return intermediateYear; }
    public void setYear(String year) { this.intermediateYear = year; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getStudentCount() { return totalStudents; }
    public void setStudentCount(long studentCount) { this.totalStudents = studentCount; }

    public String getAssignedFacultyName() { return assignedFacultyName; }
    public void setAssignedFacultyName(String assignedFacultyName) { this.assignedFacultyName = assignedFacultyName; }

    public String getAssignedFaculty() { return assignedFacultyName; }
    public void setAssignedFaculty(String assignedFaculty) { this.assignedFacultyName = assignedFaculty; }

    public Long getAssignedFacultyId() { return assignedFacultyId; }
    public void setAssignedFacultyId(Long assignedFacultyId) { this.assignedFacultyId = assignedFacultyId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
