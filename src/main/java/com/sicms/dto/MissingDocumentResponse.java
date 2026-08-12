package com.sicms.dto;

public class MissingDocumentResponse {

    private String studentId;
    private String studentName;
    private String rollNumber;
    private String branchGroup;
    private String intermediateYear;
    private String section;
    private String missingDocumentCode;
    private String missingDocumentName;
    private String category;

    public MissingDocumentResponse() {
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getBranchGroup() {
        return branchGroup;
    }

    public void setBranchGroup(String branchGroup) {
        this.branchGroup = branchGroup;
    }

    public String getIntermediateYear() {
        return intermediateYear;
    }

    public void setIntermediateYear(String intermediateYear) {
        this.intermediateYear = intermediateYear;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getMissingDocumentCode() {
        return missingDocumentCode;
    }

    public void setMissingDocumentCode(String missingDocumentCode) {
        this.missingDocumentCode = missingDocumentCode;
    }

    public String getMissingDocumentName() {
        return missingDocumentName;
    }

    public void setMissingDocumentName(String missingDocumentName) {
        this.missingDocumentName = missingDocumentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
