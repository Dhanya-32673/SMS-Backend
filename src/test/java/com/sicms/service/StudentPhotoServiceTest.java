package com.sicms.service;

import com.sicms.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

public class StudentPhotoServiceTest {

    private StudentPhotoService photoService;

    @BeforeEach
    void setUp() {
        photoService = new StudentPhotoService();
    }

    @Test
    @DisplayName("validatePhoto: Rejects empty or null file")
    void testValidatePhoto_Empty() {
        assertThrows(AuthException.class, () -> photoService.validatePhoto(null));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        assertThrows(AuthException.class, () -> photoService.validatePhoto(emptyFile));
    }

    @Test
    @DisplayName("validatePhoto: Rejects disallowed file types")
    void testValidatePhoto_InvalidType() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "dummy pdf content".getBytes());
        assertThrows(AuthException.class, () -> photoService.validatePhoto(pdfFile));

        MockMultipartFile textFile = new MockMultipartFile("file", "file.txt", "text/plain", "dummy text".getBytes());
        assertThrows(AuthException.class, () -> photoService.validatePhoto(textFile));
    }

    @Test
    @DisplayName("validatePhoto: Rejects files larger than 5 MB")
    void testValidatePhoto_TooLarge() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);
        assertThrows(AuthException.class, () -> photoService.validatePhoto(largeFile));
    }

    @Test
    @DisplayName("validatePhoto: Accepts valid JPEG and PNG files within 5 MB")
    void testValidatePhoto_Valid() {
        MockMultipartFile jpegFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertDoesNotThrow(() -> photoService.validatePhoto(jpegFile));

        MockMultipartFile pngFile = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        assertDoesNotThrow(() -> photoService.validatePhoto(pngFile));
    }

    @Test
    @DisplayName("generatePhotoPath: Generates safe path with canonical studentId, timestamp, and extension")
    void testGeneratePhotoPath() {
        String path = photoService.generatePhotoPath("STU2026001001", "my_avatar.png");
        assertNotNull(path);
        assertTrue(path.startsWith("students/STU2026001001/"), "Path must start with students/{studentId}/");
        assertTrue(path.endsWith(".png"), "Path must preserve valid extension");

        // Disallowed extensions should default to .jpg
        String fallbackPath = photoService.generatePhotoPath("STU2026001001", "script.exe");
        assertTrue(fallbackPath.endsWith(".jpg"), "Unrecognized extension must fallback to .jpg");
    }

    @Test
    @DisplayName("extractStoragePathFromUrl: Extracts clean path from public Supabase URL")
    void testExtractStoragePath() {
        String publicUrl = "https://ookzjdmkoaunbrufvmvq.supabase.co/storage/v1/object/public/student-profile-photos/students/STU2026001001/photo.jpg";
        String extracted = photoService.extractStoragePathFromUrl(publicUrl);
        assertEquals("students/STU2026001001/photo.jpg", extracted);
    }

    @Test
    @DisplayName("getPhotoContentType: Correctly determines MIME type")
    void testGetPhotoContentType() {
        assertEquals("image/png", photoService.getPhotoContentType("https://example.com/photo.png"));
        assertEquals("image/jpeg", photoService.getPhotoContentType("https://example.com/photo.jpg"));
        assertEquals("image/jpeg", photoService.getPhotoContentType("https://example.com/photo.jpeg"));
        assertEquals("image/jpeg", photoService.getPhotoContentType(null));
    }
}
