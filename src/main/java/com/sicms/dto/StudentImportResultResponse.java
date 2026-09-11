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
}
