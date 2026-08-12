package com.sicms.exception;

public class DuplicateCertificateException extends RuntimeException {

    private final Long existingCertificateId;
    private final String certificateType;
    private final String studentId;

    public DuplicateCertificateException(Long existingCertificateId, String certificateType, String studentId) {
        super("Certificate already exists.");
        this.existingCertificateId = existingCertificateId;
        this.certificateType = certificateType;
        this.studentId = studentId;
    }

    public Long getExistingCertificateId() {
        return existingCertificateId;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public String getStudentId() {
        return studentId;
    }
}
