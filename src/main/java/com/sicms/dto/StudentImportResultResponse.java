package com.sicms.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentImportResultResponse {

    private int totalRows;
    private int importedCount;
    private int updatedCount;
    private int skippedCount;
    private int failedCount;
    private String message;
    private List<StudentImportRowDto> failedRows = new ArrayList<>();

    private String targetCampus;
    private String targetGroup;
    private String targetYear;
    private String targetSection;
    private String targetAcademicYear;
    private String creatorRole;

    public StudentImportResultResponse() {
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<StudentImportRowDto> getFailedRows() { return failedRows; }
    public void setFailedRows(List<StudentImportRowDto> failedRows) { this.failedRows = failedRows; }

    public String getTargetCampus() { return targetCampus; }
    public void setTargetCampus(String targetCampus) { this.targetCampus = targetCampus; }

    public String getTargetGroup() { return targetGroup; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }

    public String getTargetYear() { return targetYear; }
    public void setTargetYear(String targetYear) { this.targetYear = targetYear; }

    public String getTargetSection() { return targetSection; }
    public void setTargetSection(String targetSection) { this.targetSection = targetSection; }

    public String getTargetAcademicYear() { return targetAcademicYear; }
    public void setTargetAcademicYear(String targetAcademicYear) { this.targetAcademicYear = targetAcademicYear; }

    public String getCreatorRole() { return creatorRole; }
    public void setCreatorRole(String creatorRole) { this.creatorRole = creatorRole; }
}
