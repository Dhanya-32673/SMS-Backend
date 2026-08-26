package com.sicms.service;

import com.sicms.exception.AuthException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class StudentPhotoService {

    private static final Logger log = Logger.getLogger(StudentPhotoService.class.getName());

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String UPLOAD_ROOT = "uploads/student-photos";

    // Fallback publishable key for public storage operations
    private static final String KNOWN_PUBLISHABLE_KEY = "sb_publishable_OjgA5UN-lQri79WSRQtAHA_fctd67NO";

    @Value("${SUPABASE_URL:${supabase.url:https://ookzjdmkoaunbrufvmvq.supabase.co}}")
    private String supabaseUrl = "https://ookzjdmkoaunbrufvmvq.supabase.co";

    @Value("${SUPABASE_PUBLISHABLE_KEY:${supabase.publishable.key:}}")
    private String publishableKey;

    @Value("${SUPABASE_SECRET_KEY:${supabase.secret.key:${SUPABASE_SERVICE_ROLE_KEY:${supabase.service.role.key:}}}}")
    private String secretKey;

    @Value("${SUPABASE_STORAGE_BUCKET_PHOTOS:${supabase.storage.bucket.photos:student-profile-photos}}")
    private String storageBucket = "student-profile-photos";

    private final RestTemplate restTemplate;

    public StudentPhotoService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    public void validateConfiguration() {
        boolean hasUrl = supabaseUrl != null && !supabaseUrl.isBlank();
        List<String> candidateKeys = getCandidateAuthKeys();

        if (hasUrl && !candidateKeys.isEmpty()) {
            log.info(">>> [SUPABASE PHOTOS] Student Photo Service initialized with bucket: " + storageBucket);
        } else {
            log.warning(">>> [SUPABASE NOTICE] Student Photo Service running with fallback URL handling. Supabase auth keys unconfigured.");
        }
    }

    private boolean isValidKey(String key) {
        if (key == null || key.isBlank()) return false;
        String lower = key.trim().toLowerCase();
        return !lower.contains("your_") && !lower.contains("placeholder") && !lower.equals("null");
    }

    private String readSecretKeyFromEnvFile() {
        try {
            for (String path : Arrays.asList(".env", "../.env", "../../.env")) {
                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    for (String line : Files.readAllLines(p)) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("SUPABASE_SECRET_KEY=") || trimmed.startsWith("SUPABASE_SERVICE_ROLE_KEY=")) {
                            String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                            if (isValidKey(val)) {
                                return val;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<String> getCandidateAuthKeys() {
        List<String> keys = new ArrayList<>();
        // 1. Configured secret key
        if (isValidKey(secretKey)) {
            keys.add(secretKey.trim());
        }
        // 2. System environment variables
        String envSecret = System.getenv("SUPABASE_SECRET_KEY");
        if (isValidKey(envSecret) && !keys.contains(envSecret.trim())) {
            keys.add(envSecret.trim());
        }
        String envServiceRole = System.getenv("SUPABASE_SERVICE_ROLE_KEY");
        if (isValidKey(envServiceRole) && !keys.contains(envServiceRole.trim())) {
            keys.add(envServiceRole.trim());
        }
        // 3. Local .env file if available
        String envFileSecret = readSecretKeyFromEnvFile();
        if (isValidKey(envFileSecret) && !keys.contains(envFileSecret.trim())) {
            keys.add(envFileSecret.trim());
        }
        // 4. Configured publishable key
        if (isValidKey(publishableKey) && !keys.contains(publishableKey.trim())) {
            keys.add(publishableKey.trim());
        }
        // 5. System environment publishable key
        String envPublishable = System.getenv("SUPABASE_PUBLISHABLE_KEY");
        if (isValidKey(envPublishable) && !keys.contains(envPublishable.trim())) {
            keys.add(envPublishable.trim());
        }
        // 6. Verified fallback publishable key
        if (!keys.contains(KNOWN_PUBLISHABLE_KEY)) {
            keys.add(KNOWN_PUBLISHABLE_KEY);
        }
        return keys;
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

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read photo file bytes: " + e.getMessage());
        }

        // 1. Upload to Supabase Storage
        boolean uploaded = uploadToSupabase(generatedPath, fileBytes, file.getContentType());
        if (!uploaded) {
            log.severe("Failed to upload student photo to Supabase storage bucket: " + generatedPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload photo to storage. Please check storage connection.");
        }

        // 2. Save local disk backup
        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, generatedPath);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, fileBytes);
        } catch (IOException e) {
            log.warning("Warning: Local student photo backup copy failed: " + e.getMessage());
        }

        return getPublicUrlForPhoto(generatedPath);
    }

    public String uploadFacultyPhoto(Long facultyId, MultipartFile file) {
        validatePhoto(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String generatedPath = generateFacultyPhotoPath(facultyId, originalFilename);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read faculty photo file bytes: " + e.getMessage());
        }

        boolean uploaded = uploadToSupabase(generatedPath, fileBytes, file.getContentType());
        if (!uploaded) {
            log.severe("Failed to upload faculty photo to Supabase storage bucket: " + generatedPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload photo to storage. Please check storage connection.");
        }

        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, generatedPath);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, fileBytes);
        } catch (IOException e) {
            log.warning("Warning: Local faculty photo backup copy failed: " + e.getMessage());
        }

        return getPublicUrlForPhoto(generatedPath);
    }

    public String generatePhotoPath(String studentId, String originalFilename) {
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Arrays.asList("jpg", "jpeg", "png").contains(extension)) {
            extension = "jpg";
        }
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("students/%s/%d_%s.%s", studentId, timestamp, uuid, extension);
    }

    public String generateFacultyPhotoPath(Long facultyId, String originalFilename) {
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Arrays.asList("jpg", "jpeg", "png").contains(extension)) {
            extension = "jpg";
        }
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("faculty/%s/%d_%s.%s", facultyId, timestamp, uuid, extension);
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

    public byte[] getPhotoBytes(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return null;

        String cleanPath = extractStoragePathFromUrl(urlOrPath);

        // 1. Try reading from Supabase authenticated or public
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            List<String> candidateKeys = getCandidateAuthKeys();
            String downloadEndpoint = supabaseUrl + "/storage/v1/object/authenticated/" + storageBucket + "/" + cleanPath;
            for (String authKey : candidateKeys) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + authKey);
                    headers.set("apikey", authKey);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<byte[]> response = restTemplate.exchange(downloadEndpoint, HttpMethod.GET, entity, byte[].class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        return response.getBody();
                    }
                } catch (Exception ignored) {}
            }
            try {
                String publicEndpoint = supabaseUrl + "/storage/v1/object/public/" + storageBucket + "/" + cleanPath;
                ResponseEntity<byte[]> response = restTemplate.getForEntity(publicEndpoint, byte[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
            } catch (Exception ignored2) {}
        }

        // 2. Fallback to local disk
        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, cleanPath);
            if (Files.exists(targetFile) && Files.isRegularFile(targetFile)) {
                return Files.readAllBytes(targetFile);
            }
        } catch (IOException ignored) {}

        return null;
    }

    public String getPhotoContentType(String urlOrPath) {
        if (urlOrPath == null) return "image/jpeg";
        String lower = urlOrPath.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/jpeg";
    }

    private boolean uploadToSupabase(String storagePath, byte[] fileBytes, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return false;

        List<String> candidateKeys = getCandidateAuthKeys();
        if (candidateKeys.isEmpty()) {
            log.severe("No valid Supabase auth keys available for storage upload.");
            return false;
        }

        String uploadEndpoint = supabaseUrl + "/storage/v1/object/" + storageBucket + "/" + storagePath;
        String mimeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";

        for (String authKey : candidateKeys) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + authKey);
                headers.set("apikey", authKey);
                headers.set("x-upsert", "true");
                headers.setContentType(MediaType.parseMediaType(mimeType));

                HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(uploadEndpoint, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info(">>> SUPABASE PHOTO UPLOAD SUCCESS: " + storagePath);
                    return true;
                }
            } catch (Exception e) {
                log.warning("Supabase photo upload attempt failed for " + storagePath + ": " + e.getMessage());
            }
        }
        return false;
    }

    public String extractStoragePathFromUrl(String url) {
        if (url == null || url.isBlank()) return url;
        String bucket = (storageBucket != null && !storageBucket.isBlank()) ? storageBucket : "student-profile-photos";
        String publicMarker = "/object/public/" + bucket + "/";
        if (url.contains(publicMarker)) {
            return url.substring(url.indexOf(publicMarker) + publicMarker.length());
        }
        String authMarker = "/object/authenticated/" + bucket + "/";
        if (url.contains(authMarker)) {
            return url.substring(url.indexOf(authMarker) + authMarker.length());
        }
        String objectMarker = "/object/" + bucket + "/";
        if (url.contains(objectMarker)) {
            return url.substring(url.indexOf(objectMarker) + objectMarker.length());
        }
        if (url.startsWith(bucket + "/")) {
            return url.substring(bucket.length() + 1);
        }
        if (url.contains("/storage/v1/object/public/")) {
            String after = url.substring(url.indexOf("/storage/v1/object/public/") + "/storage/v1/object/public/".length());
            if (after.contains("/")) {
                return after.substring(after.indexOf("/") + 1);
            }
        }
        return url;
    }

    public boolean deletePhotoFile(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return false;

        String cleanPath = extractStoragePathFromUrl(urlOrPath);

        // 1. Delete from Supabase Storage
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            List<String> candidateKeys = getCandidateAuthKeys();
            String deleteEndpoint = supabaseUrl + "/storage/v1/object/" + storageBucket + "/" + cleanPath;
            for (String authKey : candidateKeys) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + authKey);
                    headers.set("apikey", authKey);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    restTemplate.exchange(deleteEndpoint, HttpMethod.DELETE, entity, String.class);
                    log.info(">>> SUPABASE PHOTO DELETE SUCCESS: " + cleanPath);
                    break;
                } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                    log.info(">>> SUPABASE PHOTO DELETE: file not found (already deleted): " + cleanPath);
                    break;
                } catch (Exception e) {
                    log.warning("Supabase photo delete notice (" + cleanPath + "): " + e.getMessage());
                }
            }
        }

        // 2. Delete local disk file
        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, cleanPath);
            Files.deleteIfExists(targetFile);
        } catch (IOException ignored) {}

        return true;
    }
}
