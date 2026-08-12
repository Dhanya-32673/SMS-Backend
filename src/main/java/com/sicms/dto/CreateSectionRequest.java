package com.sicms.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateSectionRequest {

    @NotBlank(message = "Section name is required")
    private String name;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private String branchGroup;
    private String intermediateYear;
    private Integer capacity = 60;
    private String description;
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getBranchGroup() { return branchGroup; }
    public void setBranchGroup(String branchGroup) { this.branchGroup = branchGroup; }

    public String getIntermediateYear() { return intermediateYear; }
    public void setIntermediateYear(String intermediateYear) { this.intermediateYear = intermediateYear; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
