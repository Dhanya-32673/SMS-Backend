package com.sicms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StudentQrService {

    @Value("${app.verification.base-url:https://sicms.example.com/student/verify}")
    private String verificationBaseUrl;

    public String generateQrCodePayload(String studentId) {
        return String.format("%s/%s", verificationBaseUrl, studentId);
    }

    public String generateVerificationUrl(String studentId) {
        return generateQrCodePayload(studentId);
    }
}
