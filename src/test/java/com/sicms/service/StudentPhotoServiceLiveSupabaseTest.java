package com.sicms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class StudentPhotoServiceLiveSupabaseTest {

    @Autowired
    private StudentPhotoService studentPhotoService;

    @Test
    @DisplayName("Live Test: uploadStudentPhoto uploads to Supabase and verifies existence")
    void testUploadAndVerifyPhoto() {
        // Create a 1x1 valid PNG
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };

        MockMultipartFile file = new MockMultipartFile("file", "verify_avatar.png", "image/png", pngBytes);

        String publicUrl = studentPhotoService.uploadStudentPhoto("STU2026001001", file);
        assertNotNull(publicUrl, "Public URL must not be null");
        assertTrue(publicUrl.contains("student-profile-photos"), "Public URL must reference the photos bucket");
        assertTrue(publicUrl.contains("STU2026001001"), "Public URL must contain canonical student ID");

        // Verify photo bytes can be retrieved
        byte[] retrieved = studentPhotoService.getPhotoBytes(publicUrl);
        assertNotNull(retrieved, "Must be able to fetch photo bytes from Supabase");
        assertTrue(retrieved.length > 0, "Retrieved bytes must not be empty");

        // Clean up test file from Supabase
        boolean deleted = studentPhotoService.deletePhotoFile(publicUrl);
        assertTrue(deleted, "Must delete test photo successfully");
    }
}
