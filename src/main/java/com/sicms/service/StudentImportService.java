package com.sicms.service;

import com.sicms.dto.StudentImportPreviewResponse;
import com.sicms.dto.StudentImportResultResponse;
import com.sicms.dto.StudentImportRowDto;
import com.sicms.entity.*;
import com.sicms.repository.*;
import com.sicms.util.StudentExcelImportHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StudentImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static final Set<String> OFFICIAL_CAMPUSES = Set.of(
        "TITANIC", "SUSRUTHA", "DHANVANTARI", "GIRLS", "VAIDEHI", "MEDEX",
        "AIIMS CCO", "CCO", "ABDUL KALAM", "DCO", "INDRA BHAVAN", "APARNA",
        "VISWAKARMA", "VASISTA", "GARUDA", "GCO", "ADITHYA CO", "VAARAHI"
    );

    private final StudentRepository studentRepository;
    private final AcademicGroupRepository groupRepository;
    private final AcademicSectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final StudentIdGeneratorService idGeneratorService;
    private final ExportAuditLogRepository auditLogRepository;
    private final FacultyService facultyService;
    private final FacultyAssignmentRepository assignmentRepository;
    private final StudentService studentService;

    @Autowired
    public StudentImportService(
            StudentRepository studentRepository,
            AcademicGroupRepository groupRepository,
            AcademicSectionRepository sectionRepository,
            UserRepository userRepository,
            StudentIdGeneratorService idGeneratorService,
            ExportAuditLogRepository auditLogRepository,
            @Autowired(required = false) FacultyService facultyService,
            @Autowired(required = false) FacultyAssignmentRepository assignmentRepository,
            @Autowired(required = false) StudentService studentService
    ) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.idGeneratorService = idGeneratorService;
        this.auditLogRepository = auditLogRepository;
        this.facultyService = facultyService;
        this.assignmentRepository = assignmentRepository;
        this.studentService = studentService;
    }

    public StudentImportService(
            StudentRepository studentRepository,
            AcademicGroupRepository groupRepository,
            AcademicSectionRepository sectionRepository,
            UserRepository userRepository,
            StudentIdGeneratorService idGeneratorService,
            ExportAuditLogRepository auditLogRepository
    ) {
        this(studentRepository, groupRepository, sectionRepository, userRepository, idGeneratorService, auditLogRepository, null, null, null);
    }

    /**
     * Normalizes section strings by trimming and stripping "Section " prefix if present.
     */
    public String normalizeSectionName(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("(?i)^section\\s+", "").trim().toUpperCase();
    }

    /**
     * Resolves an AcademicSection matching branchGroup, intermediateYear, and section name.
     */
    public AcademicSection resolveSection(String branchGroup, String intermediateYear, String section) {
        if (sectionRepository == null) return null;
        String cleanSec = normalizeSectionName(section);
        String cleanGrp = branchGroup != null ? branchGroup.trim().toUpperCase() : "";
        String cleanYr = intermediateYear != null ? intermediateYear.trim() : "";

        return sectionRepository.findByActiveTrue().stream()
                .filter(s -> (cleanGrp.isEmpty() || s.getBranchGroup().trim().equalsIgnoreCase(cleanGrp))
                        && (cleanYr.isEmpty() || s.getIntermediateYear().trim().equalsIgnoreCase(cleanYr))
                        && (normalizeSectionName(s.getName()).equalsIgnoreCase(cleanSec) || s.getName().trim().equalsIgnoreCase(section != null ? section.trim() : "")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Generates and streams the official blank Excel template with formatting & sample row.
     */
    public void generateTemplate(OutputStream outputStream) throws IOException {
        StudentExcelImportHelper.generateTemplate(outputStream);
    }

    /**
     * Step 1: Validates an uploaded Excel file (default/admin without destination override).
     */
    @Transactional(readOnly = true)
    public StudentImportPreviewResponse validateImport(MultipartFile file) {
        return validateAdminImport(file, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public StudentImportPreviewResponse validateAdminImport(
            MultipartFile file,
            String branchGroup,
            String intermediateYear,
            String section
    ) {
        return validateAdminImport(file, null, branchGroup, null, intermediateYear, section);
    }

    /**
     * Admin Validation: Validates uploaded Excel file with selected Campus, Group, and Academic Year.
     */
    @Transactional(readOnly = true)
    public StudentImportPreviewResponse validateAdminImport(
            MultipartFile file,
            String campus,
            String branchGroup,
            String academicYear,
            String intermediateYear,
            String section
    ) {
        if (campus != null && !campus.isBlank()) {
            String normCampus = campus.trim().toUpperCase();
            if (!OFFICIAL_CAMPUSES.contains(normCampus)) {
                StudentImportPreviewResponse res = new StudentImportPreviewResponse();
                res.getHeaderErrors().add("Selected Campus '" + campus + "' is invalid. Must be one of the official master campuses.");
                res.setCanProceed(false);
                return res;
            }
        }

        AcademicSection targetSection = null;
        if (branchGroup != null && !branchGroup.isBlank() && section != null && !section.isBlank()) {
            AcademicGroup grp = groupRepository.findByActiveTrue().stream()
                    .filter(g -> g.getCode().trim().equalsIgnoreCase(branchGroup.trim()))
                    .findFirst()
                    .orElse(null);
            if (grp == null) {
                StudentImportPreviewResponse res = new StudentImportPreviewResponse();
                res.getHeaderErrors().add("Selected Academic Group '" + branchGroup + "' does not exist or is inactive.");
                res.setCanProceed(false);
                return res;
            }

            targetSection = resolveSection(branchGroup, intermediateYear, section);
            if (targetSection == null) {
                StudentImportPreviewResponse res = new StudentImportPreviewResponse();
                res.getHeaderErrors().add("Selected Section '" + section + "' does not exist for Group '" + branchGroup + "' and Year '" + intermediateYear + "'.");
                res.setCanProceed(false);
                return res;
            }
        }

        StudentImportPreviewResponse response = validateImportInternal(file, campus, branchGroup, academicYear, targetSection, null, "ROLE_ADMIN");
        return response;
    }

    /**
     * Faculty Validation: Validates uploaded Excel file scoped to authorized section.
     */
    @Transactional(readOnly = true)
    public StudentImportPreviewResponse validateFacultyImport(
            MultipartFile file,
            Long assignmentId,
            String facultyEmail
    ) {
        if (facultyService == null || assignmentRepository == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Faculty service not available.");
        }
        Faculty faculty = facultyService.getFacultyByUserEmail(facultyEmail);
        List<FacultyAssignment> activeAssignments = assignmentRepository.findActiveByFacultyId(faculty.getId());
        if (activeAssignments == null || activeAssignments.isEmpty()) {
            StudentImportPreviewResponse res = new StudentImportPreviewResponse();
            res.getHeaderErrors().add("No active academic sections are assigned to your faculty account.");
            res.setCanProceed(false);
            return res;
        }

        FacultyAssignment targetAssignment;
        if (assignmentId != null) {
            targetAssignment = activeAssignments.stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to import students into this section."));
        } else {
            targetAssignment = activeAssignments.get(0);
        }

        AcademicSection targetSection = resolveSection(targetAssignment.getBranchGroup(), targetAssignment.getIntermediateYear(), targetAssignment.getSection());

        StudentImportPreviewResponse response = validateImportInternal(file, null, targetAssignment.getBranchGroup(), targetAssignment.getAcademicYear(), targetSection, targetAssignment, "ROLE_FACULTY");
        response.setTargetGroup(targetAssignment.getBranchGroup());
        response.setTargetYear(targetAssignment.getIntermediateYear());
        response.setTargetSection(targetAssignment.getSection());
        response.setTargetAcademicYear(targetAssignment.getAcademicYear());
        return response;
    }

    private StudentImportPreviewResponse validateImportInternal(
            MultipartFile file,
            String targetCampus,
            String targetGroup,
            String targetAcademicYear,
            AcademicSection targetSection,
            FacultyAssignment targetAssignment,
            String role
    ) {
        StudentImportPreviewResponse response = new StudentImportPreviewResponse();

        if (file == null || file.isEmpty()) {
            response.getHeaderErrors().add("Please select a non-empty Excel (.xlsx) file to upload.");
            response.setCanProceed(false);
            return response;
        }

        String originalFilename = file.getOriginalFilename();
        response.setFileName(originalFilename);
        response.setFileSize(file.getSize());

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            response.getHeaderErrors().add("Invalid file format. Only Excel files with .xlsx extension are supported.");
            response.setCanProceed(false);
            return response;
        }

        List<String> headerErrors = new ArrayList<>();
        List<StudentImportRowDto> rows;
        try {
            rows = StudentExcelImportHelper.parseExcel(file, headerErrors);
        } catch (Exception e) {
            response.getHeaderErrors().add("Failed to parse Excel file: " + e.getMessage());
            response.setCanProceed(false);
            return response;
        }

        if (!headerErrors.isEmpty()) {
            response.setHeaderErrors(headerErrors);
            response.setCanProceed(false);
            return response;
        }

        if (rows.isEmpty()) {
            response.getHeaderErrors().add("Excel file contains no data rows to import.");
            response.setCanProceed(false);
            return response;
        }

        // Preload active Academic Groups for validation
        Set<String> validGroups = groupRepository.findByActiveTrue().stream()
                .map(g -> g.getCode().trim().toUpperCase())
                .collect(Collectors.toSet());

        // Tracking duplicates within the file
        Set<String> fileAdmissions = new HashSet<>();
        Set<String> fileStudentIds = new HashSet<>();

        int validCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;

        for (StudentImportRowDto row : rows) {
            // Apply target assignment if present
            if (targetCampus != null && !targetCampus.isBlank()) {
                row.setCampus(targetCampus.trim().toUpperCase());
            }
            if (targetGroup != null && !targetGroup.isBlank()) {
                row.setBranchGroup(targetGroup.trim());
            }
            if (targetAcademicYear != null && !targetAcademicYear.isBlank()) {
                row.setAcademicYear(targetAcademicYear.trim());
            }

            if (targetSection != null) {
                row.setBranchGroup(targetSection.getBranchGroup());
                row.setIntermediateYear(targetSection.getIntermediateYear());
                row.setSection(targetSection.getName());
                if (targetSection.getAcademicYear() != null && !targetSection.getAcademicYear().isBlank()) {
                    row.setAcademicYear(targetSection.getAcademicYear());
                }
            } else if (targetAssignment != null) {
                row.setBranchGroup(targetAssignment.getBranchGroup());
                row.setIntermediateYear(targetAssignment.getIntermediateYear());
                row.setSection(targetAssignment.getSection());
                if (targetAssignment.getAcademicYear() != null && !targetAssignment.getAcademicYear().isBlank()) {
                    row.setAcademicYear(targetAssignment.getAcademicYear());
                }
            }

            validateRow(row, validGroups, fileAdmissions, fileStudentIds);

            if ("DUPLICATE".equals(row.getStatus())) {
                duplicateCount++;
            } else if ("ERROR".equals(row.getStatus())) {
                errorCount++;
            } else {
                validCount++;
            }
        }

        response.setTotalRows(rows.size());
        response.setValidRows(validCount);
        response.setInvalidRows(errorCount);
        response.setDuplicateRows(duplicateCount);
        response.setPreview(rows);
        response.setCanProceed(validCount > 0);
        response.setTargetCampus(targetCampus);
        response.setTargetGroup(targetGroup != null ? targetGroup : (targetSection != null ? targetSection.getBranchGroup() : null));
        response.setTargetAcademicYear(targetAcademicYear != null ? targetAcademicYear : (targetSection != null ? targetSection.getAcademicYear() : null));
        if (targetSection != null) {
            response.setTargetYear(targetSection.getIntermediateYear());
            response.setTargetSection(targetSection.getName());
        }

        return response;
    }

    /**
     * Step 2: Confirms the import (default / backward compatible).
     */
    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultResponse confirmImport(
            MultipartFile file,
            boolean skipDuplicates,
            boolean updateExisting,
            String currentUserEmail
    ) {
        return confirmAdminImport(file, null, null, null, null, null, skipDuplicates, updateExisting, currentUserEmail);
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultResponse confirmAdminImport(
            MultipartFile file,
            String branchGroup,
            String intermediateYear,
            String section,
            boolean skipDuplicates,
            boolean updateExisting,
            String adminEmail
    ) {
        return confirmAdminImport(file, null, branchGroup, null, intermediateYear, section, skipDuplicates, updateExisting, adminEmail);
    }

    /**
     * Admin Confirmation: Confirms import with selected Campus, Group, and Academic Year.
     */
    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultResponse confirmAdminImport(
            MultipartFile file,
            String campus,
            String branchGroup,
            String academicYear,
            String intermediateYear,
            String section,
            boolean skipDuplicates,
            boolean updateExisting,
            String adminEmail
    ) {
        AcademicSection targetSection = null;
        if (branchGroup != null && !branchGroup.isBlank() && section != null && !section.isBlank()) {
            targetSection = resolveSection(branchGroup, intermediateYear, section);
            if (targetSection == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section '" + section + "' is invalid or does not exist for the selected Group and Year.");
            }
        }
        return confirmImportInternal(file, campus, branchGroup, academicYear, targetSection, null, skipDuplicates, updateExisting, adminEmail, "ROLE_ADMIN");
    }

    /**
     * Faculty Confirmation: Confirms import with authenticated Faculty assignment.
     */
    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultResponse confirmFacultyImport(
            MultipartFile file,
            Long assignmentId,
            boolean skipDuplicates,
            boolean updateExisting,
            String facultyEmail
    ) {
        if (facultyService == null || assignmentRepository == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Faculty service not available.");
        }
        Faculty faculty = facultyService.getFacultyByUserEmail(facultyEmail);
        List<FacultyAssignment> activeAssignments = assignmentRepository.findActiveByFacultyId(faculty.getId());
        if (activeAssignments == null || activeAssignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active academic sections assigned to your faculty account.");
        }

        FacultyAssignment targetAssignment;
        if (assignmentId != null) {
            targetAssignment = activeAssignments.stream()
                    .filter(a -> a.getId().equals(assignmentId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to import students into this section."));
        } else {
            targetAssignment = activeAssignments.get(0);
        }

        AcademicSection targetSection = resolveSection(targetAssignment.getBranchGroup(), targetAssignment.getIntermediateYear(), targetAssignment.getSection());

        return confirmImportInternal(file, null, targetAssignment.getBranchGroup(), targetAssignment.getAcademicYear(), targetSection, targetAssignment, skipDuplicates, updateExisting, facultyEmail, "ROLE_FACULTY");
    }

    private StudentImportResultResponse confirmImportInternal(
            MultipartFile file,
            String targetCampus,
            String targetGroup,
            String targetAcademicYear,
            AcademicSection targetSection,
            FacultyAssignment targetAssignment,
            boolean skipDuplicates,
            boolean updateExisting,
            String currentUserEmail,
            String role
    ) {
        StudentImportResultResponse result = new StudentImportResultResponse();

        if (file == null || file.isEmpty()) {
            result.setMessage("No file uploaded for confirmation.");
            return result;
        }

        List<String> headerErrors = new ArrayList<>();
        List<StudentImportRowDto> rows;
        try {
            rows = StudentExcelImportHelper.parseExcel(file, headerErrors);
        } catch (Exception e) {
            result.setMessage("Failed to parse Excel file: " + e.getMessage());
            return result;
        }

        if (!headerErrors.isEmpty() || rows.isEmpty()) {
            result.setMessage("Invalid Excel file or no data rows found.");
            return result;
        }

        User creatorUser = currentUserEmail != null
                ? userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null)
                : null;

        Set<String> validGroups = groupRepository.findByActiveTrue().stream()
                .map(g -> g.getCode().trim().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> fileAdmissions = new HashSet<>();
        Set<String> fileStudentIds = new HashSet<>();

        int importedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<StudentImportRowDto> failedRows = new ArrayList<>();

        for (StudentImportRowDto row : rows) {
            if (targetCampus != null && !targetCampus.isBlank()) {
                row.setCampus(targetCampus.trim().toUpperCase());
            }
            if (targetGroup != null && !targetGroup.isBlank()) {
                row.setBranchGroup(targetGroup.trim());
            }
            if (targetAcademicYear != null && !targetAcademicYear.isBlank()) {
                row.setAcademicYear(targetAcademicYear.trim());
            }

            if (targetSection != null) {
                row.setBranchGroup(targetSection.getBranchGroup());
                row.setIntermediateYear(targetSection.getIntermediateYear());
                row.setSection(targetSection.getName());
                if (targetSection.getAcademicYear() != null && !targetSection.getAcademicYear().isBlank()) {
                    row.setAcademicYear(targetSection.getAcademicYear());
                }
            } else if (targetAssignment != null) {
                row.setBranchGroup(targetAssignment.getBranchGroup());
                row.setIntermediateYear(targetAssignment.getIntermediateYear());
                row.setSection(targetAssignment.getSection());
                if (targetAssignment.getAcademicYear() != null && !targetAssignment.getAcademicYear().isBlank()) {
                    row.setAcademicYear(targetAssignment.getAcademicYear());
                }
            }

            validateRow(row, validGroups, fileAdmissions, fileStudentIds);

            if ("ERROR".equals(row.getStatus())) {
                failedCount++;
                failedRows.add(row);
                continue;
            }

            if ("DUPLICATE".equals(row.getStatus())) {
                if (updateExisting) {
                    try {
                        updateExistingStudent(row, creatorUser);
                        updatedCount++;
                    } catch (Exception ex) {
                        row.addError("Update failed: " + ex.getMessage());
                        failedCount++;
                        failedRows.add(row);
                    }
                } else if (skipDuplicates) {
                    skippedCount++;
                } else {
                    failedCount++;
                    failedRows.add(row);
                }
                continue;
            }

            // Create new student
            try {
                createNewStudent(row, creatorUser);
                importedCount++;
            } catch (Exception ex) {
                row.addError("Creation failed: " + ex.getMessage());
                failedCount++;
                failedRows.add(row);
            }
        }

        result.setTotalRows(rows.size());
        result.setImportedCount(importedCount);
        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setFailedCount(failedCount);
        result.setFailedRows(failedRows);
        result.setCreatorRole(role);

        String grp = targetGroup != null ? targetGroup : (targetSection != null ? targetSection.getBranchGroup() : (targetAssignment != null ? targetAssignment.getBranchGroup() : null));
        String yr = targetSection != null ? targetSection.getIntermediateYear() : (targetAssignment != null ? targetAssignment.getIntermediateYear() : null);
        String sec = targetSection != null ? targetSection.getName() : (targetAssignment != null ? targetAssignment.getSection() : null);
        String ay = targetAcademicYear != null ? targetAcademicYear : (targetSection != null ? targetSection.getAcademicYear() : (targetAssignment != null ? targetAssignment.getAcademicYear() : null));

        result.setTargetCampus(targetCampus);
        result.setTargetGroup(grp);
        result.setTargetYear(yr);
        result.setTargetSection(sec);
        result.setTargetAcademicYear(ay);

        if (targetCampus != null && !targetCampus.isBlank()) {
            if (grp != null && ay != null) {
                result.setMessage(String.format("%d students imported successfully to %s — %s — %s.", importedCount, targetCampus, grp, ay));
            } else {
                result.setMessage(String.format("%d students imported successfully to %s campus.", importedCount, targetCampus));
            }
        } else {
            result.setMessage(String.format("Import completed: %d created, %d updated, %d skipped, %d failed.",
                    importedCount, updatedCount, skippedCount, failedCount));
        }

        // Audit Logging
        try {
            String destDesc = (grp != null && sec != null) ? " [" + grp + "-" + yr + "-" + sec + "]" : "";
            ExportAuditLog log = new ExportAuditLog(
                    creatorUser != null ? creatorUser.getId() : null,
                    currentUserEmail != null ? currentUserEmail : role,
                    role,
                    "BULK_IMPORT: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.xlsx") + destDesc,
                    importedCount + updatedCount,
                    "SYSTEM"
            );
            auditLogRepository.save(log);
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Streams the error report workbook for download.
     */
    public void generateErrorReport(List<StudentImportRowDto> failedRows, OutputStream outputStream) throws IOException {
        StudentExcelImportHelper.generateErrorReport(failedRows, outputStream);
    }

    // ==========================================
    // Internal Validation and Mapping Helpers
    // ==========================================

    private void validateRow(
            StudentImportRowDto row,
            Set<String> validGroups,
            Set<String> fileAdmissions,
            Set<String> fileStudentIds
    ) {
        // 1. Student ID
        if (!isBlank(row.getStudentId())) {
            String stuId = row.getStudentId().trim().toUpperCase();
            if (!fileStudentIds.add(stuId)) {
                row.markDuplicate("Duplicate Student ID '" + row.getStudentId() + "' within Excel file.");
            } else if (studentRepository.existsByStudentId(row.getStudentId().trim())) {
                row.markDuplicate("Student ID '" + row.getStudentId() + "' already exists in database.");
            }
        }

        // 2. Admission Number
        if (!isBlank(row.getAdmissionNumber())) {
            String adm = row.getAdmissionNumber().trim().toUpperCase();
            if (!fileAdmissions.add(adm)) {
                row.markDuplicate("Duplicate Admission Number '" + row.getAdmissionNumber() + "' within Excel file.");
            } else if (studentRepository.existsByAdmissionNumberIgnoreCase(row.getAdmissionNumber().trim())) {
                row.markDuplicate("Student with Admission Number '" + row.getAdmissionNumber() + "' already exists in database.");
            }
        }

        // 3. Full Name
        if (isBlank(row.getFullName())) {
            row.addError("Full Name is required.");
        }

        // 4. Gender
        if (isBlank(row.getGender())) {
            row.addError("Gender is required.");
        } else {
            String g = row.getGender().trim().toUpperCase();
            if (!"MALE".equals(g) && !"FEMALE".equals(g) && !"OTHER".equals(g)) {
                row.addError("Invalid Gender '" + row.getGender() + "'. Must be MALE, FEMALE, or OTHER.");
            } else {
                row.setGender(g);
            }
        }

        // 5. Date of Birth
        if (row.getDateOfBirth() == null) {
            row.addError("Date of Birth is required and must be valid format (DD-MM-YYYY or YYYY-MM-DD).");
        } else if (!row.getDateOfBirth().isBefore(LocalDate.now())) {
            row.addError("Date of Birth must be in the past.");
        }

        // 6. Nationality (optional, default Indian)
        if (isBlank(row.getNationality())) {
            row.setNationality("Indian");
        }

        // 7. Religion (optional)
        // 8. Category (optional)

        // 9. Aadhaar Number (optional, exactly 12 digits if provided)
        if (!isBlank(row.getAadhaarNumber())) {
            String cleanAadhaar = row.getAadhaarNumber().replaceAll("[^0-9]", "");
            if (cleanAadhaar.length() != 12) {
                row.addError("Aadhaar Number must contain exactly 12 digits.");
            } else {
                row.setAadhaarNumber(cleanAadhaar);
            }
        }

        // 10. Profile Photo URL (optional)

        // 11. Mobile Number (required, 10-15 digits)
        if (isBlank(row.getMobileNumber())) {
            row.addError("Mobile Number is required.");
        } else {
            String cleanMobile = row.getMobileNumber().replaceAll("[^0-9]", "");
            if (cleanMobile.length() < 10 || cleanMobile.length() > 15) {
                row.addError("Mobile Number must be a valid 10-digit number.");
            } else {
                row.setMobileNumber(cleanMobile);
            }
        }

        // 12. Alternate Mobile (optional, 10-15 digits)
        if (!isBlank(row.getAlternateMobile())) {
            String cleanAlt = row.getAlternateMobile().replaceAll("[^0-9]", "");
            if (cleanAlt.length() < 10 || cleanAlt.length() > 15) {
                row.addWarning("Alternate Mobile should be 10 digits.");
            }
            row.setAlternateMobile(cleanAlt);
        }

        // 13. Email Address - 1 (required, valid format)
        if (isBlank(row.getEmailAddress1())) {
            row.addError("Primary email address is required to create Student Portal account.");
        } else if (!EMAIL_PATTERN.matcher(row.getEmailAddress1().trim()).matches()) {
            row.addError("Invalid Email Address - 1: '" + row.getEmailAddress1() + "'.");
        } else if (userRepository.existsByEmailIgnoreCase(row.getEmailAddress1().trim())) {
            row.addError("This email address ('" + row.getEmailAddress1().trim() + "') is already registered with another account.");
        }

        // 14. Email Address - 2 (required, valid format)
        if (isBlank(row.getEmailAddress2())) {
            row.addError("Email Address - 2 is required.");
        } else if (!EMAIL_PATTERN.matcher(row.getEmailAddress2().trim()).matches()) {
            row.addError("Invalid Email Address - 2: '" + row.getEmailAddress2() + "'.");
        }

        // 15. Father Name (required)
        if (isBlank(row.getFatherName())) {
            row.addError("Father Name is required.");
        }

        // 16. Mother Name (required)
        if (isBlank(row.getMotherName())) {
            row.addError("Mother Name is required.");
        }

        // 17. Academic Year (required)
        if (isBlank(row.getAcademicYear())) {
            row.addError("Academic Year is required (e.g. 2026-2027).");
        }

        // 18. Branch / Group (required)
        if (isBlank(row.getBranchGroup())) {
            row.addError("Branch / Group is required (e.g. MPC, BiPC, MEC, CEC, HEC).");
        } else {
            String grp = row.getBranchGroup().trim().toUpperCase();
            if (!validGroups.isEmpty() && !validGroups.contains(grp)) {
                row.addError("Academic Group '" + row.getBranchGroup() + "' does not exist in master records.");
            } else {
                row.setBranchGroup(grp);
            }
        }

        // 19. Intermediate Year (required)
        if (isBlank(row.getIntermediateYear())) {
            row.addError("Intermediate Year is required (1st Year / 2nd Year).");
        } else {
            String yr = row.getIntermediateYear().trim();
            if (yr.equalsIgnoreCase("1") || yr.equalsIgnoreCase("1st") || yr.equalsIgnoreCase("first year")) {
                row.setIntermediateYear("1st Year");
            } else if (yr.equalsIgnoreCase("2") || yr.equalsIgnoreCase("2nd") || yr.equalsIgnoreCase("second year")) {
                row.setIntermediateYear("2nd Year");
            }
        }

        // 20. Batch (required)
        if (isBlank(row.getBatch())) {
            row.addError("Batch is required (e.g. 2026-2028).");
        }

        // 21. Admission Type (optional, default REGULAR)
        if (isBlank(row.getAdmissionType())) {
            row.setAdmissionType("REGULAR");
        }

        // 22. Hostel / Day Scholar (required)
        if (isBlank(row.getHostelDayScholar())) {
            row.setHostelDayScholar("DAY_SCHOLAR");
        } else {
            String h = row.getHostelDayScholar().trim().toUpperCase().replace(" ", "_");
            if (!"DAY_SCHOLAR".equals(h) && !"HOSTEL".equals(h) && !"HOSTELLER".equals(h)) {
                row.addError("Hostel / Day Scholar must be DAY_SCHOLAR or HOSTEL.");
            } else {
                if ("HOSTELLER".equals(h)) h = "HOSTEL";
                row.setHostelDayScholar(h);
            }
        }
    }

    private void createNewStudent(StudentImportRowDto row, User adminUser) {
        Student student = new Student();

        // 1. Student ID
        String stuId = row.getStudentId();
        if (isBlank(stuId)) {
            stuId = idGeneratorService.generateStudentId();
        }
        student.setStudentId(stuId.trim());

        // 2. Admission Number
        student.setAdmissionNumber(!isBlank(row.getAdmissionNumber()) ? row.getAdmissionNumber().trim() : null);

        // 3. Full Name
        student.setFullName(row.getFullName().trim());

        // 4. Gender
        student.setGender(row.getGender());

        // 5. Date of Birth
        student.setDateOfBirth(row.getDateOfBirth());

        // 6. Nationality
        student.setNationality(row.getNationality() != null ? row.getNationality() : "Indian");

        // 7. Religion
        student.setReligion(row.getReligion());

        // 8. Category
        student.setCategory(row.getCategory());

        // 9. Aadhaar Number
        student.setAadhaarNumber(row.getAadhaarNumber());

        // 10. Profile Photo URL
        student.setProfilePhotoUrl(row.getProfilePhotoUrl());

        // 11. Mobile Number
        student.setMobileNumber(row.getMobileNumber());

        // 12. Alternate Mobile
        student.setAlternateMobile(row.getAlternateMobile());

        // 13. Email Address - 1
        student.setEmailAddress1(row.getEmailAddress1().trim());

        // 14. Email Address - 2
        student.setEmailAddress2(row.getEmailAddress2().trim());

        // 15. Father Name
        student.setFatherName(row.getFatherName().trim());

        // 16. Mother Name
        student.setMotherName(row.getMotherName().trim());

        // 17. Academic Year
        student.setAcademicYear(row.getAcademicYear().trim());

        // 18. Branch / Group
        student.setBranchGroup(row.getBranchGroup().trim());

        // 19. Intermediate Year
        student.setIntermediateYear(row.getIntermediateYear().trim());

        // 20. Batch
        student.setBatch(row.getBatch().trim());

        // 21. Admission Type
        student.setAdmissionType(row.getAdmissionType());

        // 22. Hostel / Day Scholar
        student.setHostelDayScholar(row.getHostelDayScholar());

        // 23. Campus
        if (!isBlank(row.getCampus())) {
            student.setCampus(row.getCampus().trim());
        }

        // Technical fields
        student.setStatus(StudentStatus.ACTIVE);
        student.setSection(row.getSection() != null ? row.getSection() : "Unassigned");
        student.setCreatedBy(adminUser);

        Student saved = studentRepository.save(student);
        if (studentService != null) {
            studentService.createStudentUserAccount(saved);
        }
    }

    private void updateExistingStudent(StudentImportRowDto row, User adminUser) {
        Student student = null;
        if (!isBlank(row.getStudentId())) {
            student = studentRepository.findByStudentId(row.getStudentId().trim()).orElse(null);
        }
        if (student == null && !isBlank(row.getAdmissionNumber())) {
            student = studentRepository.findByAdmissionNumberIgnoreCase(row.getAdmissionNumber().trim()).orElse(null);
        }

        if (student == null) {
            throw new IllegalStateException("Existing student not found for update.");
        }

        if (!isBlank(row.getFullName())) student.setFullName(row.getFullName().trim());
        if (!isBlank(row.getGender())) student.setGender(row.getGender());
        if (row.getDateOfBirth() != null) student.setDateOfBirth(row.getDateOfBirth());
        if (!isBlank(row.getNationality())) student.setNationality(row.getNationality());
        if (!isBlank(row.getReligion())) student.setReligion(row.getReligion());
        if (!isBlank(row.getCategory())) student.setCategory(row.getCategory());
        if (!isBlank(row.getAadhaarNumber())) student.setAadhaarNumber(row.getAadhaarNumber());
        if (!isBlank(row.getProfilePhotoUrl())) student.setProfilePhotoUrl(row.getProfilePhotoUrl());
        if (!isBlank(row.getMobileNumber())) student.setMobileNumber(row.getMobileNumber());
        if (!isBlank(row.getAlternateMobile())) student.setAlternateMobile(row.getAlternateMobile());
        if (!isBlank(row.getEmailAddress1())) student.setEmailAddress1(row.getEmailAddress1().trim());
        if (!isBlank(row.getEmailAddress2())) student.setEmailAddress2(row.getEmailAddress2().trim());
        if (!isBlank(row.getFatherName())) student.setFatherName(row.getFatherName().trim());
        if (!isBlank(row.getMotherName())) student.setMotherName(row.getMotherName().trim());
        if (!isBlank(row.getAcademicYear())) student.setAcademicYear(row.getAcademicYear().trim());
        if (!isBlank(row.getBranchGroup())) student.setBranchGroup(row.getBranchGroup().trim());
        if (!isBlank(row.getIntermediateYear())) student.setIntermediateYear(row.getIntermediateYear().trim());
        if (!isBlank(row.getBatch())) student.setBatch(row.getBatch().trim());
        if (!isBlank(row.getAdmissionType())) student.setAdmissionType(row.getAdmissionType());
        if (!isBlank(row.getHostelDayScholar())) student.setHostelDayScholar(row.getHostelDayScholar());
        if (!isBlank(row.getSection())) student.setSection(row.getSection());

        studentRepository.save(student);
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
