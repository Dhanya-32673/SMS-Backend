package com.sicms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentStorageService {

    @Value("${supabase.url:https://ookzjdmkoaunbrufvmvq.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.publishable.key}")
    private String publishableKey;

    @Value("${supabase.secret.key:${supabase.service.role.key}}")
    private String secretKey;

    @Value("${supabase.storage.bucket.documents:student-documents}")
    private String bucketName;

    private static final String UPLOAD_ROOT = "uploads/student-certificates";

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final RestTemplate restTemplate;

    public DocumentStorageService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Validates that the uploaded file is a valid, non-empty PDF document under 5 MB.
     */
    public void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The PDF file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("PDF file size exceeds the maximum allowed size.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        // Verify actual PDF header magic bytes (%PDF- within first 1024 bytes per ISO 32000-1 specification)
        try (java.io.InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[1024];
            int read = is.read(buffer, 0, 1024);
            if (read < 4) {
                throw new IllegalArgumentException("The selected file is not a valid PDF document.");
            }
            String headerStr = new String(buffer, 0, read, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (!headerStr.contains("%PDF-")) {
                throw new IllegalArgumentException("The selected file is not a valid PDF document.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("The selected file is not a valid PDF document.");
        }
    }

    /**
     * Saves the uploaded file to Supabase Storage (and local disk as backup).
     * Path format: students/{studentId}/certificates/{uuid}_{originalFilename}
     */
    public String saveFile(String studentId, MultipartFile file) {
        validateDocumentFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String storagePath = "students/" + studentId + "/certificates/" + uuid + "_" + originalFilename;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes: " + e.getMessage(), e);
        }

        // 1. Try uploading to Supabase Storage REST API
        boolean supabaseUploaded = uploadToSupabase(storagePath, fileBytes, file.getContentType());
        if (!supabaseUploaded) {
            System.out.println(">>> SUPABASE STORAGE NOTICE: Primary Supabase API upload skipped or unavailable. Preserving local storage backup for: " + storagePath);
        }

        // 2. Save local backup copy for offline dev execution
        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, storagePath);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, fileBytes);
        } catch (IOException e) {
            System.err.println("Warning: Local file backup copy failed: " + e.getMessage());
        }

        return storagePath;
    }

    /**
     * Uploads raw file bytes to Supabase Storage REST API.
     */
    private boolean uploadToSupabase(String storagePath, byte[] fileBytes, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return false;

        String authKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : publishableKey;
        if (authKey == null || authKey.isBlank()) return false;

        try {
            String uploadEndpoint = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + storagePath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authKey);
            headers.set("apikey", authKey);
            headers.set("x-upsert", "true");
            if (contentType != null && !contentType.isBlank()) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadEndpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(">>> SUPABASE STORAGE UPLOAD SUCCESS: " + storagePath);
                return true;
            }
            return false;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println(">>> SUPABASE STORAGE REST UPLOAD FAILED (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println(">>> SUPABASE STORAGE UPLOAD NOTICE (" + storagePath + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a stored file from Supabase Storage and the local disk backup.
     * Returns true on success.
     * Throws RuntimeException if the Supabase DELETE API call fails (non-404 error).
     * A 404 (file already deleted / not found) is treated as a success to be idempotent.
     */
    public boolean deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return false;

        // 1. Delete from Supabase Storage REST API
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            String authKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : publishableKey;
            if (authKey != null && !authKey.isBlank()) {
                try {
                    String deleteEndpoint = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + storagePath;
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + authKey);
                    headers.set("apikey", authKey);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    restTemplate.exchange(deleteEndpoint, org.springframework.http.HttpMethod.DELETE, entity, String.class);
                    System.out.println(">>> SUPABASE STORAGE DELETE SUCCESS: " + storagePath);
                } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                    // File already gone — treat as success (idempotent)
                    System.out.println(">>> SUPABASE STORAGE DELETE: file not found (already deleted): " + storagePath);
                } catch (Exception e) {
                    System.err.println(">>> SUPABASE STORAGE DELETE FAILED (" + storagePath + "): " + e.getMessage());
                    throw new RuntimeException("Failed to delete document from Supabase Storage: " + storagePath + " — " + e.getMessage(), e);
                }
            }
        }

        // 2. Delete local backup file (best-effort, never throws)
        try {
            Path targetFile = Paths.get(UPLOAD_ROOT, storagePath);
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            System.err.println(">>> LOCAL FILE DELETE NOTICE (" + storagePath + "): " + e.getMessage());
        }
        return true;
    }


    /**
     * Checks if file exists in Supabase Storage or local disk.
     */
    public boolean fileExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return false;

        // Check local disk
        Path filePath = Paths.get(UPLOAD_ROOT, storagePath);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            return true;
        }

        // Check Supabase Storage
        byte[] supabaseBytes = downloadFromSupabase(storagePath);
        return supabaseBytes != null && supabaseBytes.length > 0;
    }

    /**
     * Reads stored file bytes from Supabase Storage (or local disk fallback).
     */
    public byte[] readFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new RuntimeException("Storage path is empty.");
        }

        // 1. Try reading from Supabase Storage REST API
        byte[] supabaseBytes = downloadFromSupabase(storagePath);
        if (supabaseBytes != null && supabaseBytes.length > 0) {
            return supabaseBytes;
        }

        // 2. Fallback to local disk storage
        try {
            Path filePath = Paths.get(UPLOAD_ROOT, storagePath);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
        } catch (IOException e) {
            System.err.println("Failed reading local fallback file: " + e.getMessage());
        }

        throw new RuntimeException("File not found in storage: " + storagePath);
    }

    /**
     * Downloads file bytes from Supabase Storage REST API.
     */
    private byte[] downloadFromSupabase(String storagePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return null;

        String authKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : publishableKey;
        if (authKey == null || authKey.isBlank()) return null;

        try {
            String downloadEndpoint = supabaseUrl + "/storage/v1/object/authenticated/" + bucketName + "/" + storagePath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authKey);
            headers.set("apikey", authKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(downloadEndpoint, HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            // Try public bucket URL if authenticated endpoint is restricted
            try {
                String publicEndpoint = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + storagePath;
                ResponseEntity<byte[]> pubResponse = restTemplate.getForEntity(publicEndpoint, byte[].class);
                if (pubResponse.getStatusCode().is2xxSuccessful() && pubResponse.getBody() != null) {
                    return pubResponse.getBody();
                }
            } catch (Exception ex) {
                // Ignore fallback exception
            }
        }
        return null;
    }

    public String getContentType(String storagePath) {
        if (storagePath == null) return "application/octet-stream";
        String lower = storagePath.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    public String getBucketName() {
        return bucketName;
    }
}
