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
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

    private final StudentRepository studentRepository;
    private final AcademicGroupRepository groupRepository;
    private final AcademicSectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final StudentIdGeneratorService idGeneratorService;
    private final ExportAuditLogRepository auditLogRepository;
    private final FacultyService facultyService;
    private final FacultyAssignmentRepository assignmentRepository;

    @Autowired
    public StudentImportService(
            StudentRepository studentRepository,
            AcademicGroupRepository groupRepository,
            AcademicSectionRepository sectionRepository,
            UserRepository userRepository,
            StudentIdGeneratorService idGeneratorService,
            ExportAuditLogRepository auditLogRepository,
            @Autowired(required = false) FacultyService facultyService,
            @Autowired(required = false) FacultyAssignmentRepository assignmentRepository
    ) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.idGeneratorService = idGeneratorService;
        this.auditLogRepository = auditLogRepository;
        this.facultyService = facultyService;
        this.assignmentRepository = assignmentRepository;
    }

    public StudentImportService(
            StudentRepository studentRepository,
            AcademicGroupRepository groupRepository,
            AcademicSectionRepository sectionRepository,
            UserRepository userRepository,
            StudentIdGeneratorService idGeneratorService,
            ExportAuditLogRepository auditLogRepository
    ) {
        this(studentRepository, groupRepository, sectionRepository, userRepository, idGeneratorService, auditLogRepository, null, null);
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
        return validateAdminImport(file, null, null, null);
    }

    /**
     * Admin Validation: Validates uploaded Excel file with selected Group, Year, and Section.
     */
    @Transactional(readOnly = true)
    public StudentImportPreviewResponse validateAdminImport(
            MultipartFile file,
            String branchGroup,
            String intermediateYear,
            String section
    ) {
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

        StudentImportPreviewResponse response = validateImportInternal(file, targetSection, null, "ROLE_ADMIN");
        if (targetSection != null) {
            response.setTargetGroup(targetSection.getBranchGroup());
            response.setTargetYear(targetSection.getIntermediateYear());
            response.setTargetSection(targetSection.getName());
            response.setTargetAcademicYear(targetSection.getAcademicYear());
        } else if (branchGroup != null && !branchGroup.isBlank()) {
            response.setTargetGroup(branchGroup);
            response.setTargetYear(intermediateYear);
            response.setTargetSection(section);
        }
        response.setRole("ROLE_ADMIN");
        return response;
    }

    /**
     * Faculty Validation: Validates uploaded Excel file using the authenticated faculty member's assigned section.
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active academic sections assigned to your faculty account. Please contact administrator.");
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

        StudentImportPreviewResponse response = validateImportInternal(file, targetSection, targetAssignment, "ROLE_FACULTY");
        response.setTargetGroup(targetAssignment.getBranchGroup());
        response.setTargetYear(targetAssignment.getIntermediateYear());
        response.setTargetSection(targetAssignment.getSection());
        response.setTargetAcademicYear(targetAssignment.getAcademicYear());
        response.setAssignedFacultyName(faculty.getFullName());
        response.setRole("ROLE_FACULTY");
        return response;
    }

    private StudentImportPreviewResponse validateImportInternal(
            MultipartFile file,
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

        // Preload active Academic Groups and Sections for fast O(1) resolution
        Set<String> validGroups = groupRepository.findByActiveTrue().stream()
                .map(g -> g.getCode().trim().toUpperCase())
                .collect(Collectors.toSet());

        List<AcademicSection> allSections = sectionRepository.findByActiveTrue();
        Set<String> validSectionNames = allSections.stream()
                .map(s -> s.getName().trim().toUpperCase())
                .collect(Collectors.toSet());

        // Tracking duplicates within the file
        Set<String> fileRolls = new HashSet<>();
        Set<String> fileAdmissions = new HashSet<>();
        Set<String> fileStudentIds = new HashSet<>();

        int validCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;

        for (StudentImportRowDto row : rows) {
            // Apply target assignment if present
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

            validateRow(row, validGroups, validSectionNames, allSections, fileRolls, fileAdmissions, fileStudentIds);

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
        return confirmAdminImport(file, null, null, null, skipDuplicates, updateExisting, currentUserEmail);
    }

    /**
     * Admin Confirmation: Confirms import with selected Group, Year, and Section.
     */
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
        AcademicSection targetSection = null;
        if (branchGroup != null && !branchGroup.isBlank() && section != null && !section.isBlank()) {
            targetSection = resolveSection(branchGroup, intermediateYear, section);
            if (targetSection == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section '" + section + "' is invalid or does not exist for the selected Group and Year.");
            }
        }
        return confirmImportInternal(file, targetSection, null, skipDuplicates, updateExisting, adminEmail, "ROLE_ADMIN");
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

        return confirmImportInternal(file, targetSection, targetAssignment, skipDuplicates, updateExisting, facultyEmail, "ROLE_FACULTY");
    }

    private StudentImportResultResponse confirmImportInternal(
            MultipartFile file,
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

        List<AcademicSection> allSections = sectionRepository.findByActiveTrue();
        Set<String> validSectionNames = allSections.stream()
                .map(s -> s.getName().trim().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> fileRolls = new HashSet<>();
        Set<String> fileAdmissions = new HashSet<>();
        Set<String> fileStudentIds = new HashSet<>();

        int importedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<StudentImportRowDto> failedRows = new ArrayList<>();

        for (StudentImportRowDto row : rows) {
            // Apply target assignment if present
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

            validateRow(row, validGroups, validSectionNames, allSections, fileRolls, fileAdmissions, fileStudentIds);

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

        String grp = targetSection != null ? targetSection.getBranchGroup() : (targetAssignment != null ? targetAssignment.getBranchGroup() : null);
        String yr = targetSection != null ? targetSection.getIntermediateYear() : (targetAssignment != null ? targetAssignment.getIntermediateYear() : null);
        String sec = targetSection != null ? targetSection.getName() : (targetAssignment != null ? targetAssignment.getSection() : null);
        String ay = targetSection != null ? targetSection.getAcademicYear() : (targetAssignment != null ? targetAssignment.getAcademicYear() : null);

        result.setTargetGroup(grp);
        result.setTargetYear(yr);
        result.setTargetSection(sec);
        result.setTargetAcademicYear(ay);

        result.setMessage(String.format("Import completed: %d created, %d updated, %d skipped, %d failed.",
                importedCount, updatedCount, skippedCount, failedCount));

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
            Set<String> validSectionNames,
            List<AcademicSection> allSections,
            Set<String> fileRolls,
            Set<String> fileAdmissions,
            Set<String> fileStudentIds
    ) {
        // 1. Required Personal Fields
        if (isBlank(row.getRollNumber())) {
            row.addError("Roll Number is required.");
        } else {
            String roll = row.getRollNumber().trim().toUpperCase();
            if (!fileRolls.add(roll)) {
                row.markDuplicate("Duplicate Roll Number '" + row.getRollNumber() + "' within Excel file.");
            } else if (studentRepository.existsByRollNumberIgnoreCase(row.getRollNumber().trim())) {
                row.markDuplicate("Student with Roll Number '" + row.getRollNumber() + "' already exists in database.");
            }
        }

        if (!isBlank(row.getAdmissionNumber())) {
            String adm = row.getAdmissionNumber().trim().toUpperCase();
            if (!fileAdmissions.add(adm)) {
                row.markDuplicate("Duplicate Admission Number '" + row.getAdmissionNumber() + "' within Excel file.");
            } else if (studentRepository.existsByAdmissionNumberIgnoreCase(row.getAdmissionNumber().trim())) {
                row.markDuplicate("Student with Admission Number '" + row.getAdmissionNumber() + "' already exists in database.");
            }
        }

        if (!isBlank(row.getStudentId())) {
            String stuId = row.getStudentId().trim().toUpperCase();
            if (!fileStudentIds.add(stuId)) {
                row.markDuplicate("Duplicate Student ID '" + row.getStudentId() + "' within Excel file.");
            } else if (studentRepository.existsByStudentId(row.getStudentId().trim())) {
                row.markDuplicate("Student ID '" + row.getStudentId() + "' already exists in database.");
            }
        }

        if (isBlank(row.getFirstName())) {
            row.addError("First Name is required.");
        }
        if (isBlank(row.getLastName())) {
            row.addError("Last Name is required.");
        }

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

        if (row.getDateOfBirth() == null) {
            row.addError("Date of Birth is required and must be valid format (DD-MM-YYYY or YYYY-MM-DD).");
        } else if (!row.getDateOfBirth().isBefore(LocalDate.now())) {
            row.addError("Date of Birth must be in the past.");
        }

        // Optional Personal Identifiers Validation
        if (!isBlank(row.getAadhaarNumber())) {
            String cleanAadhaar = row.getAadhaarNumber().replaceAll("[^0-9]", "");
            if (cleanAadhaar.length() != 12) {
                row.addError("Aadhaar Number must be exactly 12 digits.");
            } else {
                row.setAadhaarNumber(cleanAadhaar);
            }
        }

        if (!isBlank(row.getPanNumber())) {
            String cleanPan = row.getPanNumber().trim().toUpperCase();
            if (!PAN_PATTERN.matcher(cleanPan).matches()) {
                row.addError("Invalid PAN Number format '" + row.getPanNumber() + "' (expected ABCDE1234F).");
            } else {
                row.setPanNumber(cleanPan);
            }
        }

        // 2. Required Contact Details
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

        if (!isBlank(row.getAlternateMobile())) {
            String cleanAlt = row.getAlternateMobile().replaceAll("[^0-9]", "");
            if (cleanAlt.length() < 10 || cleanAlt.length() > 15) {
                row.addWarning("Alternate Mobile should be 10 digits.");
            }
            row.setAlternateMobile(cleanAlt);
        }

        if (isBlank(row.getEmail())) {
            row.addError("Email Address is required.");
        } else if (!EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            row.addError("Invalid Email Address format: '" + row.getEmail() + "'.");
        }

        if (isBlank(row.getAddress())) {
            row.addError("Residential Address is required.");
        }
        if (isBlank(row.getCity())) {
            row.setCity("Hyderabad");
        }
        if (isBlank(row.getDistrict())) {
            row.setDistrict("Hyderabad");
        }
        if (isBlank(row.getState())) {
            row.setState("Telangana");
        }
        if (isBlank(row.getPinCode())) {
            row.setPinCode("500001");
        } else {
            String cleanPin = row.getPinCode().replaceAll("[^0-9]", "");
            if (cleanPin.length() != 6) {
                row.addWarning("PIN Code usually contains 6 digits.");
            }
            row.setPinCode(cleanPin);
        }

        // 3. Required Parent Details
        if (isBlank(row.getFatherName())) {
            row.addError("Father Name is required.");
        }
        if (isBlank(row.getMotherName())) {
            row.addError("Mother Name is required.");
        }
        if (isBlank(row.getParentMobile())) {
            row.addError("Parent Mobile is required.");
        } else {
            String cleanParentMobile = row.getParentMobile().replaceAll("[^0-9]", "");
            if (cleanParentMobile.length() < 10 || cleanParentMobile.length() > 15) {
                row.addError("Parent Mobile must be a valid 10-digit number.");
            } else {
                row.setParentMobile(cleanParentMobile);
            }
        }

        // 4. Required Academic Details
        if (isBlank(row.getAcademicYear())) {
            row.addError("Academic Year is required (e.g. 2026-2027).");
        }

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

        if (isBlank(row.getSection())) {
            row.addError("Section is required (e.g. A, B, C, D).");
        } else {
            String sec = row.getSection().trim().replaceAll("(?i)^section\\s+", "").trim().toUpperCase();
            if (sec.startsWith("SECTION ")) {
                sec = sec.substring(8).trim();
            }
            if (!validSectionNames.isEmpty() && !validSectionNames.contains(sec)) {
                row.addError("Section '" + row.getSection() + "' does not exist in master records.");
            } else {
                row.setSection(sec);
            }
        }

        if (isBlank(row.getBatch())) {
            row.addError("Batch is required (e.g. 2026-2028).");
        }

        if (row.getAdmissionDate() == null) {
            row.addError("Admission Date is required.");
        }

        if (isBlank(row.getHostelDayScholar())) {
            row.setHostelDayScholar("DAY_SCHOLAR");
        } else {
            String h = row.getHostelDayScholar().trim().toUpperCase().replace(" ", "_");
            if (!"DAY_SCHOLAR".equals(h) && !"HOSTELLER".equals(h)) {
                row.addError("Hostel / Day Scholar must be DAY_SCHOLAR or HOSTELLER.");
            } else {
                row.setHostelDayScholar(h);
            }
        }
    }

    private void createNewStudent(StudentImportRowDto row, User adminUser) {
        Student student = new Student();

        // 1. Assign unique Student ID
        String stuId = row.getStudentId();
        if (isBlank(stuId)) {
            stuId = idGeneratorService.generateStudentId();
        }
        student.setStudentId(stuId.trim());

        student.setRollNumber(row.getRollNumber().trim());
        student.setAdmissionNumber(!isBlank(row.getAdmissionNumber()) ? row.getAdmissionNumber().trim() : null);
        student.setFirstName(row.getFirstName().trim());
        student.setMiddleName(!isBlank(row.getMiddleName()) ? row.getMiddleName().trim() : null);
        student.setLastName(row.getLastName().trim());
        student.setFullName(row.getFullName());
        student.setGender(row.getGender());
        student.setDateOfBirth(row.getDateOfBirth());
        student.setBloodGroup(row.getBloodGroup());
        student.setNationality(row.getNationality());
        student.setReligion(row.getReligion());
        student.setCasteCategory(row.getCasteCategory());
        student.setAadhaarNumber(row.getAadhaarNumber());
        student.setPanNumber(row.getPanNumber());
        student.setIdentificationMarks(row.getIdentificationMarks());
        student.setProfilePhotoUrl(row.getProfilePhotoUrl());

        try {
            student.setStatus(StudentStatus.valueOf(row.getStudentStatus().toUpperCase()));
        } catch (Exception e) {
            student.setStatus(StudentStatus.ACTIVE);
        }

        student.setCreatedBy(adminUser);

        // 2. Contact Detail
        StudentContactDetail contact = new StudentContactDetail();
        contact.setMobileNumber(row.getMobileNumber());
        contact.setAlternateMobile(row.getAlternateMobile());
        contact.setEmail(row.getEmail());
        contact.setAddress(row.getAddress());
        contact.setCity(row.getCity());
        contact.setDistrict(row.getDistrict());
        contact.setState(row.getState());
        contact.setPinCode(row.getPinCode());
        contact.setCountry(row.getCountry());
        student.setContactDetail(contact);

        // 3. Parent Detail
        StudentParentDetail parent = new StudentParentDetail();
        parent.setFatherName(row.getFatherName());
        parent.setMotherName(row.getMotherName());
        parent.setParentMobile(row.getParentMobile());
        parent.setParentEmail(row.getParentEmail());
        parent.setOccupation(row.getOccupation());
        parent.setAnnualIncome(row.getAnnualIncome());
        student.setParentDetail(parent);

        // 4. Academic Detail
        StudentAcademicDetail academic = new StudentAcademicDetail();
        academic.setAcademicYear(row.getAcademicYear());
        academic.setDepartment(row.getDepartment());
        academic.setBranchGroup(row.getBranchGroup());
        academic.setIntermediateYear(row.getIntermediateYear());
        academic.setSemester(row.getSemester());
        academic.setSection(row.getSection());
        academic.setBatch(row.getBatch());
        academic.setAdmissionDate(row.getAdmissionDate());
        academic.setAdmissionType(row.getAdmissionType());
        academic.setHostelDayScholar(row.getHostelDayScholar());
        academic.setMedium(row.getMedium());
        academic.setRegulation(row.getRegulation());
        academic.setUniversityId(row.getUniversityId());
        student.setAcademicDetail(academic);

        // 5. Guardian Detail (if provided)
        if (!isBlank(row.getGuardianName()) || !isBlank(row.getGuardianMobile())) {
            StudentGuardian guardian = new StudentGuardian();
            guardian.setGuardianName(row.getGuardianName());
            guardian.setGuardianMobile(row.getGuardianMobile());
            guardian.setRelationship(row.getGuardianRelation());
            guardian.setGuardianAddress(row.getGuardianAddress());
            guardian.setFatherName(row.getFatherName());
            guardian.setMotherName(row.getMotherName());
            guardian.setStudent(student);
            student.setGuardianDetail(guardian);
        }

        studentRepository.save(student);
    }

    private void updateExistingStudent(StudentImportRowDto row, User adminUser) {
        Student student = null;
        if (!isBlank(row.getStudentId())) {
            student = studentRepository.findByStudentId(row.getStudentId().trim()).orElse(null);
        }
        if (student == null && !isBlank(row.getRollNumber())) {
            student = studentRepository.findByRollNumberIgnoreCase(row.getRollNumber().trim()).orElse(null);
        }
        if (student == null && !isBlank(row.getAdmissionNumber())) {
            student = studentRepository.findByAdmissionNumberIgnoreCase(row.getAdmissionNumber().trim()).orElse(null);
        }

        if (student == null) {
            throw new IllegalStateException("Existing student not found for update.");
        }

        student.setFirstName(row.getFirstName().trim());
        if (!isBlank(row.getMiddleName())) student.setMiddleName(row.getMiddleName().trim());
        student.setLastName(row.getLastName().trim());
        student.setFullName(row.getFullName());
        student.setGender(row.getGender());
        student.setDateOfBirth(row.getDateOfBirth());
        if (!isBlank(row.getBloodGroup())) student.setBloodGroup(row.getBloodGroup());
        if (!isBlank(row.getNationality())) student.setNationality(row.getNationality());
        if (!isBlank(row.getReligion())) student.setReligion(row.getReligion());
        if (!isBlank(row.getCasteCategory())) student.setCasteCategory(row.getCasteCategory());
        if (!isBlank(row.getAadhaarNumber())) student.setAadhaarNumber(row.getAadhaarNumber());
        if (!isBlank(row.getPanNumber())) student.setPanNumber(row.getPanNumber());
        if (!isBlank(row.getIdentificationMarks())) student.setIdentificationMarks(row.getIdentificationMarks());
        if (!isBlank(row.getProfilePhotoUrl())) student.setProfilePhotoUrl(row.getProfilePhotoUrl());

        // Contact
        StudentContactDetail contact = student.getContactDetail();
        if (contact == null) {
            contact = new StudentContactDetail();
            student.setContactDetail(contact);
        }
        contact.setMobileNumber(row.getMobileNumber());
        if (!isBlank(row.getAlternateMobile())) contact.setAlternateMobile(row.getAlternateMobile());
        contact.setEmail(row.getEmail());
        contact.setAddress(row.getAddress());
        contact.setCity(row.getCity());
        contact.setDistrict(row.getDistrict());
        contact.setState(row.getState());
        contact.setPinCode(row.getPinCode());
        contact.setCountry(row.getCountry());

        // Parent
        StudentParentDetail parent = student.getParentDetail();
        if (parent == null) {
            parent = new StudentParentDetail();
            student.setParentDetail(parent);
        }
        parent.setFatherName(row.getFatherName());
        parent.setMotherName(row.getMotherName());
        parent.setParentMobile(row.getParentMobile());
        if (!isBlank(row.getParentEmail())) parent.setParentEmail(row.getParentEmail());
        if (!isBlank(row.getOccupation())) parent.setOccupation(row.getOccupation());
        if (row.getAnnualIncome() != null) parent.setAnnualIncome(row.getAnnualIncome());

        // Academic
        StudentAcademicDetail academic = student.getAcademicDetail();
        if (academic == null) {
            academic = new StudentAcademicDetail();
            student.setAcademicDetail(academic);
        }
        academic.setAcademicYear(row.getAcademicYear());
        academic.setDepartment(row.getDepartment());
        academic.setBranchGroup(row.getBranchGroup());
        academic.setIntermediateYear(row.getIntermediateYear());
        if (row.getSemester() != null) academic.setSemester(row.getSemester());
        academic.setSection(row.getSection());
        academic.setBatch(row.getBatch());
        academic.setAdmissionDate(row.getAdmissionDate());
        if (!isBlank(row.getAdmissionType())) academic.setAdmissionType(row.getAdmissionType());
        academic.setHostelDayScholar(row.getHostelDayScholar());
        if (!isBlank(row.getMedium())) academic.setMedium(row.getMedium());
        if (!isBlank(row.getRegulation())) academic.setRegulation(row.getRegulation());
        if (!isBlank(row.getUniversityId())) academic.setUniversityId(row.getUniversityId());

        studentRepository.save(student);
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
