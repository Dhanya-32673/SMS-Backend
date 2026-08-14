package com.sicms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sicms.dto.DocumentRejectionRequest;
import com.sicms.dto.DocumentResponse;
import com.sicms.dto.DocumentSummaryResponse;
import com.sicms.dto.MissingDocumentResponse;
import com.sicms.dto.PaginatedStudentResponse;
import com.sicms.dto.StudentCertificateSummaryResponse;
import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentStatus;
import com.sicms.entity.StudentDocument;
import com.sicms.service.DocumentService;
import com.sicms.service.DocumentStorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final DocumentStorageService storageService;

    @Autowired
    public DocumentController(DocumentService documentService,
                              DocumentStorageService storageService) {
        this.documentService = documentService;
        this.storageService = storageService;
    }

    /**
     * Upload document (ADMIN & FACULTY)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("studentId") String studentId,
            @RequestParam("documentTypeId") Long documentTypeId,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "issuedBy", required = false) String issuedBy,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String uploadedBy = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        DocumentResponse response = documentService.uploadDocument(
        studentId, documentTypeId, documentNumber, issueDate, expiryDate, issuedBy, notes, file, uploadedBy,
        userDetails != null ? userDetails.getUsername() : null,
        isFaculty(userDetails)
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Replace existing certificate PDF (ADMIN & FACULTY)
     */
    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> replaceDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String uploadedBy = userDetails != null ? userDetails.getUsername() : "SYSTEM";
            DocumentResponse response = documentService.replaceDocument(id, file, uploadedBy,
                    userDetails != null ? userDetails.getUsername() : null,
                    isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete certificate (ADMIN & FACULTY)
     */
    @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentResponse response = documentService.getDocumentById(id, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    /**
     * Get all documents for a specific student
     */
    @PutMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> replaceDocumentPut(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String uploadedBy = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        DocumentResponse response = documentService.replaceDocument(id, file, uploadedBy,
                userDetails != null ? userDetails.getUsername() : null,
                isFaculty(userDetails));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Map<String, Object>> debugStudentDocuments(@PathVariable String studentId) {
        log.info("Executing document diagnostics for studentId={}", studentId);
        Map<String, Object> debugInfo = documentService.debugStudentDocuments(studentId);
        return ResponseEntity.ok(debugInfo);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByStudent(@PathVariable String studentId,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Loading documents for studentId={}", studentId);
        List<DocumentResponse> list = documentService.getDocumentsByStudent(studentId, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(list);
    }

    /**
     * Get Student Certificate Summaries (One row per student) (ADMIN & FACULTY)
     */
    @GetMapping("/student-summaries")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<PaginatedStudentResponse<StudentCertificateSummaryResponse>> getStudentSummaries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "studentId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        PaginatedStudentResponse<StudentCertificateSummaryResponse> response = documentService.getStudentSummaries(
        page, size, group, year, section, status, search, sortBy, sortDir,
        userDetails != null ? userDetails.getUsername() : null,
        isFaculty(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Search & Filter documents (ADMIN & FACULTY)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<PaginatedStudentResponse<DocumentSummaryResponse>> filterAndSearchDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) Long documentTypeId,
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PaginatedStudentResponse<DocumentSummaryResponse> response = documentService.filterAndSearchDocuments(
        page, size, studentId, documentTypeId, category, status, search, sortBy, sortDir,
        userDetails != null ? userDetails.getUsername() : null,
        isFaculty(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Verify document (ADMIN & FACULTY)
     */
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> verifyDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String verifiedBy = userDetails != null ? userDetails.getUsername() : "ADMIN";
        DocumentResponse response = documentService.verifyDocument(id, verifiedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject document (ADMIN & FACULTY)
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<DocumentResponse> rejectDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRejectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String rejectedBy = userDetails != null ? userDetails.getUsername() : "ADMIN";
        DocumentResponse response = documentService.rejectDocument(id, request.getReason(), rejectedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Archive document (ADMIN ONLY)
     */
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> archiveDocument(@PathVariable Long id) {
        DocumentResponse response = documentService.archiveDocument(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get missing required documents list (ADMIN & FACULTY)
     */
    @GetMapping("/missing")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<List<MissingDocumentResponse>> getMissingDocuments(@AuthenticationPrincipal UserDetails userDetails) {
        List<MissingDocumentResponse> list = documentService.getMissingDocuments(userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        return ResponseEntity.ok(list);
    }

    /**
     * Serve document file bytes for preview and download.
     * GET /api/documents/{id}/file?download=true (optional param for attachment)
     */
    @GetMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<?> serveDocumentFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        StudentDocument doc = documentService.getDocumentEntityById(id, userDetails != null ? userDetails.getUsername() : null, isFaculty(userDetails));
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"DOCUMENT_NOT_FOUND\",\"message\":\"Document record not found in system.\"}");
        }

        if (!storageService.fileExists(doc.getStoragePath())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"DOCUMENT_FILE_NOT_FOUND\",\"message\":\"Document file was not found in storage. Please upload the document again.\"}");
        }

        byte[] fileBytes = storageService.readFile(doc.getStoragePath());
        String contentType = storageService.getContentType(doc.getStoragePath());
        String filename = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : "document";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (download) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            // inline so browser can preview PDF or image in iframe/img
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }
        headers.setContentLength(fileBytes.length);

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    private boolean isFaculty(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream().anyMatch(authority -> "ROLE_FACULTY".equalsIgnoreCase(authority.getAuthority()));
    }
}
