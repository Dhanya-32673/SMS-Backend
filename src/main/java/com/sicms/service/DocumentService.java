package com.sicms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sicms.dto.DocumentResponse;
import com.sicms.dto.DocumentSummaryResponse;
import com.sicms.dto.MissingDocumentResponse;
import com.sicms.dto.PaginatedStudentResponse;
import com.sicms.dto.StudentCertificateSummaryResponse;
import com.sicms.entity.DocumentCategory;
import com.sicms.entity.DocumentStatus;
import com.sicms.entity.DocumentType;
import com.sicms.entity.DocumentVersion;
import com.sicms.entity.Faculty;
import com.sicms.entity.Student;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentStatus;
import com.sicms.repository.DocumentTypeRepository;
import com.sicms.repository.DocumentVersionRepository;
import com.sicms.repository.StudentDocumentRepository;
import com.sicms.repository.StudentRepository;

@Service
public class DocumentService {

    private final StudentDocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentVersionRepository versionRepository;
    private final StudentRepository studentRepository;
    private final DocumentStorageService storageService;
    private final FacultyService facultyService;

    @Autowired
    public DocumentService(
            StudentDocumentRepository documentRepository,
            DocumentTypeRepository documentTypeRepository,
            DocumentVersionRepository versionRepository,
            StudentRepository studentRepository,
            DocumentStorageService storageService,
            FacultyService facultyService) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.versionRepository = versionRepository;
        this.studentRepository = studentRepository;
        this.storageService = storageService;
        this.facultyService = facultyService;
    }

    @CacheEvict(value = {"studentSummaries", "adminDashboard", "facultyDashboard"}, allEntries = true)
    @Transactional
    public DocumentResponse uploadDocument(
            String studentId,
            Long documentTypeId,
            String documentNumber,
            LocalDate issueDate,
            LocalDate expiryDate,
            String issuedBy,
            String notes,
            MultipartFile file,
            String uploadedBy,
            String currentUserEmail,
            boolean facultyScoped) {

        storageService.validateDocumentFile(file);

        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new com.sicms.exception.StudentNotFoundException("Student not found with ID: " + studentId));

        ensureStudentAccess(student, currentUserEmail, facultyScoped);

        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new RuntimeException("Document type not found with ID: " + documentTypeId));

        // Enforce duplicate check with storage verification
        Optional<StudentDocument> existingOpt = documentRepository.findFirstByStudent_StudentIdAndDocumentType_IdOrderByIdDesc(
                studentId, documentTypeId
        );
        if (existingOpt.isPresent()) {
            StudentDocument existing = existingOpt.get();
            boolean fileInStorage = existing.getStoragePath() != null && storageService.fileExists(existing.getStoragePath());
            boolean isArchived = existing.getStatus() == DocumentStatus.ARCHIVED;

            if (!fileInStorage || isArchived || existing.getStoragePath() == null || existing.getStoragePath().isBlank()) {
                // File missing or soft deleted -> overwrite existing record
                String newStoragePath = storageService.saveFile(studentId, file);
                existing.setStoragePath(newStoragePath);
                existing.setOriginalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
                existing.setMimeType(file.getContentType());
                existing.setFileSize(file.getSize());
                existing.setStatus(DocumentStatus.UPLOADED);
                if (documentNumber != null) existing.setDocumentNumber(documentNumber);
                if (issueDate != null) existing.setIssueDate(issueDate);
                if (expiryDate != null) existing.setExpiryDate(expiryDate);
                if (issuedBy != null) existing.setIssuedBy(issuedBy);
                if (notes != null) existing.setNotes(notes);
                existing.setUploadedBy(uploadedBy);
                existing.setUploadedAt(LocalDateTime.now());
                existing.setUpdatedAt(LocalDateTime.now());

                StudentDocument saved = documentRepository.save(existing);
                return mapToResponse(saved);
            } else {
                throw new com.sicms.exception.DuplicateCertificateException(
                        existing.getId(),
                        documentType.getName(),
                        student.getStudentId()
                );
            }
        }

        String storagePath = storageService.saveFile(studentId, file);

        StudentDocument doc = new StudentDocument();
        doc.setStudent(student);
        doc.setDocumentType(documentType);
        doc.setDocumentNumber(documentNumber);
        doc.setStoragePath(storagePath);
        doc.setOriginalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        doc.setStoredFileName(UUID.randomUUID().toString());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setIssueDate(issueDate);
        doc.setExpiryDate(expiryDate);
        doc.setIssuedBy(issuedBy);
        doc.setNotes(notes);
        doc.setUploadedBy(uploadedBy);

        StudentDocument saved = documentRepository.save(doc);
        return mapToResponse(saved);
    }

    @Transactional
    public DocumentResponse replaceDocument(
            Long id,
            MultipartFile file,
            String uploadedBy,
            String currentUserEmail,
            boolean facultyScoped) {

        storageService.validateDocumentFile(file);

        StudentDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));

        ensureStudentAccess(doc.getStudent(), currentUserEmail, facultyScoped);

        // Archive old version
        DocumentVersion version = new DocumentVersion();
        version.setStudentDocument(doc);
        version.setStoragePath(doc.getStoragePath());
        version.setOriginalFileName(doc.getOriginalFileName());
        version.setStoredFileName(doc.getStoredFileName());
        version.setMimeType(doc.getMimeType());
        version.setFileSize(doc.getFileSize());
        version.setUploadedBy(doc.getUploadedBy());
        version.setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt() : LocalDateTime.now());
        versionRepository.save(version);

        // Delete old file from storage
        if (doc.getStoragePath() != null) {
            storageService.deleteFile(doc.getStoragePath());
        }

        // Save new file to storage
        String newStoragePath = storageService.saveFile(doc.getStudent().getStudentId(), file);

        doc.setStoragePath(newStoragePath);
        doc.setOriginalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        doc.setStoredFileName(UUID.randomUUID().toString());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        StudentDocument saved = documentRepository.save(doc);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id) {
        StudentDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));

        if (doc.getStoragePath() != null && !doc.getStoragePath().isBlank()) {
            try {
                storageService.deleteFile(doc.getStoragePath());
            } catch (Exception e) {
                System.err.println(">>> STORAGE DELETE NOTICE: " + e.getMessage());
            }
        }

        // Permanently delete record from PostgreSQL / Supabase
        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id, String currentUserEmail, boolean facultyScoped) {
        StudentDocument doc = getAccessibleDocumentById(id, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        return mapToResponse(doc);
    }

    @Transactional(readOnly = true)
    public StudentDocument getDocumentEntityById(Long id, String currentUserEmail, boolean facultyScoped) {
        return getAccessibleDocumentById(id, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByStudent(String studentId, String currentUserEmail, boolean facultyScoped) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new com.sicms.exception.StudentNotFoundException("Student not found with ID: " + studentId));

        ensureStudentAccess(student, currentUserEmail, facultyScoped);

        return documentRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedStudentResponse<DocumentSummaryResponse> filterAndSearchDocuments(
            int page, int size, String studentId, Long documentTypeId, DocumentCategory category,
            DocumentStatus status, String search, String sortBy, String sortDir,
            String currentUserEmail,
            boolean facultyScoped) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<StudentDocument> docPage;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            docPage = documentRepository.filterAndSearchDocumentsForFaculty(
                faculty.getId(), studentId, documentTypeId, category, status, search, pageable
            );
        } else {
            docPage = documentRepository.filterAndSearchDocuments(
                studentId, documentTypeId, category, status, search, pageable
            );
        }

        List<DocumentSummaryResponse> summaries = docPage.getContent().stream().map(d -> {
            DocumentSummaryResponse r = new DocumentSummaryResponse();
            r.setId(d.getId());
            r.setStudentId(d.getStudent().getStudentId());
            r.setStudentName(d.getStudent().getFullName());
            r.setRollNumber(d.getStudent().getRollNumber());
            r.setDocumentTypeName(d.getDocumentType().getName());
            r.setCategory(d.getDocumentType().getCategory());
            r.setOriginalFileName(d.getOriginalFileName());
            r.setMimeType(d.getMimeType());
            r.setFileSize(d.getFileSize());
            r.setStatus(d.getStatus());
            r.setUploadedBy(d.getUploadedBy());
            r.setUploadedAt(d.getUploadedAt());
            return r;
        }).collect(Collectors.toList());

        return new PaginatedStudentResponse<>(
                summaries,
                docPage.getNumber(),
                docPage.getSize(),
                docPage.getTotalElements(),
                docPage.getTotalPages(),
                docPage.isLast()
        );
    }

    @Transactional
    public DocumentResponse verifyDocument(Long id, String verifiedBy) {
        StudentDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));

        doc.setStatus(DocumentStatus.VERIFIED);
        doc.setVerifiedBy(verifiedBy);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setRejectedBy(null);
        doc.setRejectedAt(null);
        doc.setRejectionReason(null);

        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse rejectDocument(Long id, String reason, String rejectedBy) {
        StudentDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setRejectedBy(rejectedBy);
        doc.setRejectedAt(LocalDateTime.now());
        doc.setRejectionReason(reason);

        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse archiveDocument(Long id) {
        StudentDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));

        doc.setStatus(DocumentStatus.ARCHIVED);
        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<MissingDocumentResponse> getMissingDocuments(String currentUserEmail, boolean facultyScoped) {
        List<DocumentType> requiredTypes = documentTypeRepository.findByActiveTrue()
                .stream().filter(DocumentType::isRequiredByDefault).collect(Collectors.toList());

        List<Student> activeStudents;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            activeStudents = studentRepository.findAccessibleStudentsByFaculty(faculty.getId())
                    .stream().filter(s -> s.getStatus() == StudentStatus.ACTIVE).collect(Collectors.toList());
        } else {
            activeStudents = studentRepository.findAll()
                    .stream().filter(s -> s.getStatus() == StudentStatus.ACTIVE).collect(Collectors.toList());
        }

        // Batch-load only active document states to avoid N+1 queries in loop
        List<StudentDocument> allDocs = documentRepository.findByStatusIn(List.of(DocumentStatus.UPLOADED, DocumentStatus.PENDING, DocumentStatus.VERIFIED));
        Map<Long, Set<Long>> studentUploadedTypeIdsMap = allDocs.stream()
                .filter(d -> d.getStudent() != null && d.getStudent().getId() != null
                        && d.getStatus() != DocumentStatus.REJECTED && d.getStatus() != DocumentStatus.ARCHIVED
                        && d.getDocumentType() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getStudent().getId(),
                        Collectors.mapping(d -> d.getDocumentType().getId(), Collectors.toSet())
                ));

        List<MissingDocumentResponse> missingList = new ArrayList<>();

        for (Student st : activeStudents) {
            Set<Long> uploadedTypeIds = studentUploadedTypeIdsMap.getOrDefault(st.getId(), java.util.Collections.emptySet());

            for (DocumentType reqType : requiredTypes) {
                if (!uploadedTypeIds.contains(reqType.getId())) {
                    MissingDocumentResponse res = new MissingDocumentResponse();
                    res.setStudentId(st.getStudentId());
                    res.setStudentName(st.getFullName());
                    res.setRollNumber(st.getRollNumber());
                    if (st.getAcademicDetail() != null) {
                        res.setBranchGroup(st.getAcademicDetail().getBranchGroup());
                        res.setIntermediateYear(st.getAcademicDetail().getIntermediateYear());
                        res.setSection(st.getAcademicDetail().getSection());
                    }
                    res.setMissingDocumentCode(reqType.getCode());
                    res.setMissingDocumentName(reqType.getName());
                    res.setCategory(reqType.getCategory().name());
                    missingList.add(res);
                }
            }
        }

        return missingList;
    }

    public long countMissingDocuments(String currentUserEmail, boolean facultyScoped) {
        if (!facultyScoped) {
            return documentRepository.countTotalMissingDocumentsFast();
        }

        List<DocumentType> requiredTypes = documentTypeRepository.findAll().stream()
                .filter(dt -> dt.isActive() && dt.isRequiredByDefault())
                .collect(Collectors.toList());
        if (requiredTypes.isEmpty()) return 0;

        List<Student> activeStudents;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            activeStudents = studentRepository.findAccessibleStudentsByFaculty(faculty.getId())
                    .stream().filter(s -> s.getStatus() == StudentStatus.ACTIVE).collect(Collectors.toList());
        } else {
            activeStudents = studentRepository.findAllWithAcademicDetail()
                    .stream().filter(s -> s.getStatus() == StudentStatus.ACTIVE).collect(Collectors.toList());
        }

        List<StudentDocument> allDocs = documentRepository.findByStatusIn(List.of(DocumentStatus.UPLOADED, DocumentStatus.PENDING, DocumentStatus.VERIFIED));
        Map<Long, Set<Long>> studentUploadedTypeIdsMap = allDocs.stream()
                .filter(d -> d.getStudent() != null && d.getStudent().getId() != null
                        && d.getStatus() != DocumentStatus.REJECTED && d.getStatus() != DocumentStatus.ARCHIVED
                        && d.getDocumentType() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getStudent().getId(),
                        Collectors.mapping(d -> d.getDocumentType().getId(), Collectors.toSet())
                ));

        long count = 0;
        for (Student st : activeStudents) {
            Set<Long> uploadedTypeIds = studentUploadedTypeIdsMap.getOrDefault(st.getId(), java.util.Collections.emptySet());
            for (DocumentType reqType : requiredTypes) {
                if (!uploadedTypeIds.contains(reqType.getId())) {
                    count++;
                }
            }
        }
        return count;
    }

        @Transactional(readOnly = true)
    public PaginatedStudentResponse<StudentCertificateSummaryResponse> getStudentSummaries(
            int page, int size, String group, String year, String section, String status, String search, String sortBy, String sortDir,
            String currentUserEmail,
            boolean facultyScoped) {

        List<Student> allStudents;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            allStudents = studentRepository.findAccessibleStudentsByFaculty(faculty.getId());
        } else {
            allStudents = studentRepository.findAllWithAcademicDetail();
        }

        List<DocumentType> requiredTypes = documentTypeRepository.findAll().stream()
                .filter(dt -> dt.isActive() && dt.isRequiredByDefault())
                .collect(Collectors.toList());
        int totalRequired = requiredTypes.isEmpty() ? 5 : requiredTypes.size();

        // Batch-load only active certificate states to avoid N+1 queries in loop
        List<StudentDocument> allDocsForSummaries = documentRepository.findByStatusIn(List.of(DocumentStatus.UPLOADED, DocumentStatus.PENDING, DocumentStatus.VERIFIED));
        Map<String, List<StudentDocument>> docsByStudentIdMap = allDocsForSummaries.stream()
                .filter(d -> d.getStudent() != null && d.getStudent().getStudentId() != null)
                .collect(Collectors.groupingBy(d -> d.getStudent().getStudentId()));

        List<StudentCertificateSummaryResponse> summaries = new ArrayList<>();

        for (Student st : allStudents) {
            String bGroup = st.getAcademicDetail() != null ? st.getAcademicDetail().getBranchGroup() : "";
            String iYear = st.getAcademicDetail() != null ? st.getAcademicDetail().getIntermediateYear() : "";
            String sec = st.getAcademicDetail() != null ? st.getAcademicDetail().getSection() : "";

            if (group != null && !group.trim().isEmpty() && !bGroup.equalsIgnoreCase(group.trim())) {
                continue;
            }

            if (year != null && !year.trim().isEmpty() && !iYear.equalsIgnoreCase(year.trim())) {
                continue;
            }

            if (section != null && !section.trim().isEmpty() && !sec.equalsIgnoreCase(section.trim())) {
                continue;
            }

            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean matches = (st.getStudentId() != null && st.getStudentId().toLowerCase().contains(q)) ||
                        (st.getFullName() != null && st.getFullName().toLowerCase().contains(q)) ||
                        (st.getRollNumber() != null && st.getRollNumber().toLowerCase().contains(q)) ||
                        (st.getAcademicDetail() != null && st.getAcademicDetail().getUniversityId() != null && st.getAcademicDetail().getUniversityId().toLowerCase().contains(q));
                if (!matches) continue;
            }

            List<StudentDocument> docs = docsByStudentIdMap.getOrDefault(st.getStudentId(), java.util.Collections.emptyList());

            Set<Long> uploadedTypeIds = new HashSet<>();
            int verifiedCount = 0;
            int pendingCount = 0;

            for (StudentDocument doc : docs) {
                if (doc.getStatus() != DocumentStatus.ARCHIVED) {
                    uploadedTypeIds.add(doc.getDocumentType().getId());
                    if (doc.getStatus() == DocumentStatus.VERIFIED) {
                        verifiedCount++;
                    } else if (doc.getStatus() == DocumentStatus.PENDING || doc.getStatus() == DocumentStatus.UPLOADED) {
                        pendingCount++;
                    }
                }
            }

            int uploadedCount = uploadedTypeIds.size();
            int missingCount = Math.max(0, totalRequired - uploadedCount);
            double completionPercentage = totalRequired > 0 ? Math.round(((double) uploadedCount / totalRequired) * 100.0) : 100.0;

            String overallStatus;
            if (completionPercentage >= 100.0 && pendingCount == 0) {
                overallStatus = "COMPLETED";
            } else if (pendingCount > 0) {
                overallStatus = "PENDING VERIFICATION";
            } else if (uploadedCount > 0) {
                overallStatus = "PARTIALLY COMPLETED";
            } else {
                overallStatus = "NEEDS ATTENTION";
            }

            if (status != null && !status.trim().isEmpty() && !overallStatus.equalsIgnoreCase(status.trim())) {
                continue;
            }

            StudentCertificateSummaryResponse summary = new StudentCertificateSummaryResponse();
            summary.setId(st.getId());
            summary.setStudentId(st.getStudentId());
            summary.setFullName(st.getFullName());
            summary.setRollNumber(st.getRollNumber());
            summary.setAdmissionNumber(st.getAdmissionNumber() != null ? st.getAdmissionNumber() : (st.getAcademicDetail() != null ? st.getAcademicDetail().getUniversityId() : "N/A"));
            summary.setProfilePhotoUrl(st.getProfilePhotoUrl());
            summary.setBranchGroup(com.sicms.util.StudentFormatterUtil.formatBranchGroup(bGroup));
            summary.setIntermediateYear(com.sicms.util.StudentFormatterUtil.formatIntermediateYear(iYear));
            summary.setSection(com.sicms.util.StudentFormatterUtil.formatSection(sec));
            summary.setStatus(st.getStatus() != null ? st.getStatus().name() : "ACTIVE");

            summary.setTotalRequired(totalRequired);
            summary.setUploadedCount(uploadedCount);
            summary.setVerifiedCount(verifiedCount);
            summary.setPendingCount(pendingCount);
            summary.setMissingCount(missingCount);
            summary.setCompletionPercentage(completionPercentage);
            summary.setOverallStatus(overallStatus);

            summaries.add(summary);
        }

        if ("fullName".equalsIgnoreCase(sortBy)) {
            summaries.sort(Comparator.comparing(StudentCertificateSummaryResponse::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)));
        } else if ("completionPercentage".equalsIgnoreCase(sortBy)) {
            summaries.sort(Comparator.comparingDouble(StudentCertificateSummaryResponse::getCompletionPercentage));
        } else if ("pendingCount".equalsIgnoreCase(sortBy)) {
            summaries.sort(Comparator.comparingInt(StudentCertificateSummaryResponse::getPendingCount));
        } else if ("missingCount".equalsIgnoreCase(sortBy)) {
            summaries.sort(Comparator.comparingInt(StudentCertificateSummaryResponse::getMissingCount));
        } else {
            summaries.sort(Comparator.comparing(StudentCertificateSummaryResponse::getStudentId, Comparator.nullsLast(String::compareToIgnoreCase)));
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            Collections.reverse(summaries);
        }

        int totalElements = summaries.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages < 1) totalPages = 1;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<StudentCertificateSummaryResponse> pageContent = summaries.subList(fromIndex, toIndex);

        PaginatedStudentResponse<StudentCertificateSummaryResponse> response = new PaginatedStudentResponse<>();
        response.setContent(pageContent);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setLast(page >= totalPages - 1);

        return response;
    }

    private DocumentResponse mapToResponse(StudentDocument doc) {
        DocumentResponse res = new DocumentResponse();
        res.setId(doc.getId());
        res.setStudentId(doc.getStudent().getStudentId());
        res.setStudentName(doc.getStudent().getFullName());
        res.setRollNumber(doc.getStudent().getRollNumber());
        res.setDocumentTypeId(doc.getDocumentType().getId());
        res.setDocumentTypeCode(doc.getDocumentType().getCode());
        res.setDocumentTypeName(doc.getDocumentType().getName());
        res.setCategory(doc.getDocumentType().getCategory());
        res.setDocumentNumber(doc.getDocumentNumber());
        res.setStoragePath(doc.getStoragePath());
        res.setOriginalFileName(doc.getOriginalFileName());
        res.setMimeType(doc.getMimeType());
        res.setFileSize(doc.getFileSize());
        res.setStatus(doc.getStatus());
        res.setIssueDate(doc.getIssueDate());
        res.setExpiryDate(doc.getExpiryDate());
        res.setIssuedBy(doc.getIssuedBy());
        res.setUploadedBy(doc.getUploadedBy());
        res.setUploadedAt(doc.getUploadedAt());
        res.setVerifiedBy(doc.getVerifiedBy());
        res.setVerifiedAt(doc.getVerifiedAt());
        res.setRejectedBy(doc.getRejectedBy());
        res.setRejectedAt(doc.getRejectedAt());
        res.setRejectionReason(doc.getRejectionReason());
        res.setNotes(doc.getNotes());
        res.setPreviewUrl("http://localhost:8080/api/documents/" + doc.getId() + "/file");
        return res;
    }

    private java.util.Optional<StudentDocument> getAccessibleDocumentById(Long id, String currentUserEmail, boolean facultyScoped) {
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            return documentRepository.findAccessibleDocumentByFaculty(id, faculty.getId());
        }
        return documentRepository.findById(id);
    }

    private void ensureStudentAccess(Student student, String currentUserEmail, boolean facultyScoped) {
        if (!facultyScoped) {
            return;
        }

        Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
        if (!facultyService.hasAccessToStudent(faculty, student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty can only access students from assigned sections.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> debugStudentDocuments(String studentId) {
        Map<String, Object> debug = new java.util.LinkedHashMap<>();
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        debug.put("studentId", studentId);
        debug.put("studentExists", studentOpt.isPresent());

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            debug.put("studentDbId", student.getId());
            debug.put("fullName", student.getFullName());
            List<StudentDocument> docs = documentRepository.findByStudentId(student.getId());
            debug.put("documentCount", docs.size());

            List<String> paths = docs.stream().map(StudentDocument::getStoragePath).collect(Collectors.toList());
            debug.put("storageObjectPaths", paths);

            long dupCount = docs.stream()
                    .collect(Collectors.groupingBy(d -> d.getDocumentType().getId(), Collectors.counting()))
                    .values().stream().filter(c -> c > 1).count();
            debug.put("duplicateCount", dupCount);
        } else {
            debug.put("documentCount", 0);
            debug.put("storageObjectPaths", Collections.emptyList());
            debug.put("duplicateCount", 0);
        }

        debug.put("bucketReachable", storageService.isBucketReachable());
        debug.put("bucketName", storageService.getBucketName());
        return debug;
    }
}
