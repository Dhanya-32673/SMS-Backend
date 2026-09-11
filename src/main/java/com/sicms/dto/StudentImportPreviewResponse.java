package com.sicms.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentImportPreviewResponse {

    private String fileName;
    private long fileSize;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int duplicateRows;
    private boolean canProceed;
    private List<StudentImportRowDto> preview = new ArrayList<>();
    private List<String> headerErrors = new ArrayList<>();

    public StudentImportPreviewResponse() {
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }

    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }

    public boolean isCanProceed() { return canProceed; }
    public void setCanProceed(boolean canProceed) { this.canProceed = canProceed; }

    public List<StudentImportRowDto> getPreview() { return preview; }
    public void setPreview(List<StudentImportRowDto> preview) { this.preview = preview; }

    public List<String> getHeaderErrors() { return headerErrors; }
    public void setHeaderErrors(List<String> headerErrors) { this.headerErrors = headerErrors; }
}
