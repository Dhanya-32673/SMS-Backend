package com.sicms.service;

import com.sicms.exception.AuthException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class StudentPhotoService {

    private static final Logger log = Logger.getLogger(StudentPhotoService.class.getName());

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${SUPABASE_URL:${supabase.url:https://ookzjdmkoaunbrufvmvq.supabase.co}}")
    private String supabaseUrl;

    @Value("${SUPABASE_PUBLISHABLE_KEY:${supabase.publishable.key:}}")
    private String publishableKey;

    @Value("${SUPABASE_SECRET_KEY:${supabase.secret.key:${SUPABASE_SERVICE_ROLE_KEY:${supabase.service.role.key:}}}}")
    private String secretKey;

    @Value("${SUPABASE_STORAGE_BUCKET_PHOTOS:${supabase.storage.bucket.photos:student-profile-photos}}")
    private String storageBucket;

    private final RestTemplate restTemplate;

    public StudentPhotoService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    public void validateConfiguration() {
        boolean hasUrl = supabaseUrl != null && !supabaseUrl.isBlank();
        boolean hasKey = (publishableKey != null && !publishableKey.isBlank()) || (secretKey != null && !secretKey.isBlank());

        if (hasUrl && hasKey) {
            log.info(">>> [SUPABASE PHOTOS] Student Photo Service initialized with bucket: " + storageBucket);
        } else {
            log.warning(">>> [SUPABASE NOTICE] Student Photo Service running with fallback URL handling. Supabase auth keys unconfigured.");
        }
    }

    public void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AuthException("File is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AuthException("File size exceeds maximum limit of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EXTENSIONS.contains(contentType.toLowerCase())) {
            throw new AuthException("Invalid file type. Only JPG, JPEG, and PNG images are allowed");
        }
    }

    public String uploadStudentPhoto(String studentId, MultipartFile file) {
        validatePhoto(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String generatedPath = generatePhotoPath(studentId, originalFilename);

        try {
            byte[] fileBytes = file.getBytes();
            uploadToSupabase(generatedPath, fileBytes, file.getContentType());
        } catch (IOException e) {
            System.err.println("Failed to read photo file bytes: " + e.getMessage());
        }

        return getPublicUrlForPhoto(generatedPath);
    }

    public String uploadFacultyPhoto(Long facultyId, MultipartFile file) {
        validatePhoto(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String generatedPath = generateFacultyPhotoPath(facultyId, originalFilename);

        try {
            byte[] fileBytes = file.getBytes();
            uploadToSupabase(generatedPath, fileBytes, file.getContentType());
        } catch (IOException e) {
            System.err.println("Failed to read faculty photo file bytes: " + e.getMessage());
        }

        return getPublicUrlForPhoto(generatedPath);
    }

    public String generatePhotoPath(String studentId, String originalFilename) {
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return String.format("students/%s/%s.%s", studentId, UUID.randomUUID().toString().substring(0, 8), extension);
    }

    public String generateFacultyPhotoPath(Long facultyId, String originalFilename) {
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return String.format("faculty/%s/%s.%s", facultyId, UUID.randomUUID().toString().substring(0, 8), extension);
    }

    public String getPublicUrlForPhoto(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }
        if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
            return photoPath;
        }
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, storageBucket, photoPath);
    }

    private boolean uploadToSupabase(String storagePath, byte[] fileBytes, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return false;

        String authKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : publishableKey;
        if (authKey == null || authKey.isBlank()) return false;

        try {
            String uploadEndpoint = supabaseUrl + "/storage/v1/object/" + storageBucket + "/" + storagePath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authKey);
            headers.set("apikey", authKey);
            headers.set("x-upsert", "true");
            if (contentType != null && !contentType.isBlank()) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } else {
                headers.setContentType(MediaType.IMAGE_JPEG);
            }

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadEndpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(">>> SUPABASE PHOTO UPLOAD SUCCESS: " + storagePath);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println(">>> SUPABASE PHOTO UPLOAD NOTICE (" + storagePath + "): " + e.getMessage());
            return false;
        }
    }

    private String extractStoragePathFromUrl(String url) {
        if (url == null || url.isBlank()) return url;
        String publicMarker = "/object/public/" + storageBucket + "/";
        if (url.contains(publicMarker)) {
            return url.substring(url.indexOf(publicMarker) + publicMarker.length());
        }
        String authMarker = "/object/authenticated/" + storageBucket + "/";
        if (url.contains(authMarker)) {
            return url.substring(url.indexOf(authMarker) + authMarker.length());
        }
        String objectMarker = "/object/" + storageBucket + "/";
        if (url.contains(objectMarker)) {
            return url.substring(url.indexOf(objectMarker) + objectMarker.length());
        }
        if (url.startsWith(storageBucket + "/")) {
            return url.substring(storageBucket.length() + 1);
        }
        return url;
    }

    public boolean deletePhotoFile(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return false;
        if (supabaseUrl == null || supabaseUrl.isBlank()) return false;

        String authKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : publishableKey;
        if (authKey == null || authKey.isBlank()) return false;

        String cleanPath = extractStoragePathFromUrl(urlOrPath);

        try {
            String deleteEndpoint = supabaseUrl + "/storage/v1/object/" + storageBucket + "/" + cleanPath;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authKey);
            headers.set("apikey", authKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteEndpoint, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            System.out.println(">>> SUPABASE PHOTO DELETE SUCCESS: " + cleanPath);
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            System.out.println(">>> SUPABASE PHOTO DELETE: file not found (already deleted): " + cleanPath);
            return true;
        } catch (Exception e) {
            System.err.println(">>> SUPABASE PHOTO DELETE NOTICE (" + cleanPath + "): " + e.getMessage());
            return false;
        }
    }
}
