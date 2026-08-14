package com.sicms.service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.sicms.entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sicms.dto.CreateStudentRequest;
import com.sicms.dto.PaginatedStudentResponse;
import com.sicms.dto.StudentIdCardResponse;
import com.sicms.dto.StudentResponse;
import com.sicms.dto.StudentSearchResponse;
import com.sicms.dto.StudentSummaryResponse;
import com.sicms.dto.UpdateStudentRequest;
import com.sicms.entity.DocumentType;
import com.sicms.entity.Faculty;
import com.sicms.entity.Student;
import com.sicms.entity.StudentAcademicDetail;
import com.sicms.entity.StudentContactDetail;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentParentDetail;
import com.sicms.entity.StudentStatus;
import com.sicms.exception.DuplicateResourceException;
import com.sicms.exception.StudentNotFoundException;
import com.sicms.repository.DocumentTypeRepository;
import com.sicms.repository.StudentDocumentRepository;
import com.sicms.entity.ExportAuditLog;
import com.sicms.repository.ExportAuditLogRepository;
import com.sicms.repository.StudentRepository;
import com.sicms.repository.UserRepository;
import com.sicms.repository.DocumentVersionRepository;
import com.sicms.entity.DocumentVersion;
import com.sicms.util.StudentExcelExporter;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentIdGeneratorService idGeneratorService;
    private final StudentQrService qrService;
    private final StudentDocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentStorageService documentStorageService;
    private final FacultyService facultyService;
    private final StudentPhotoService photoService;
    private final DocumentTypeRepository documentTypeRepository;
    private final ExportAuditLogRepository exportAuditLogRepository;

    @Autowired
    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            StudentIdGeneratorService idGeneratorService,
            StudentQrService qrService,
            StudentDocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentStorageService documentStorageService,
            FacultyService facultyService,
            StudentPhotoService photoService,
            DocumentTypeRepository documentTypeRepository,
            ExportAuditLogRepository exportAuditLogRepository
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.idGeneratorService = idGeneratorService;
        this.qrService = qrService;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentStorageService = documentStorageService;
        this.facultyService = facultyService;
        this.photoService = photoService;
        this.documentTypeRepository = documentTypeRepository;
        this.exportAuditLogRepository = exportAuditLogRepository;
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, String createdByEmail, boolean facultyScoped) {
        if (request.getRollNumber() != null && studentRepository.existsByRollNumberIgnoreCase(request.getRollNumber().trim())) {
            throw new DuplicateResourceException("Student with Roll Number '" + request.getRollNumber() + "' already exists.");
        }

        if (request.getAdmissionNumber() != null && !request.getAdmissionNumber().isBlank()
                && studentRepository.existsByAdmissionNumberIgnoreCase(request.getAdmissionNumber().trim())) {
            throw new DuplicateResourceException("Student with Admission Number '" + request.getAdmissionNumber() + "' already exists.");
        }

        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(createdByEmail);
            enforceFacultyAcademicScope(faculty, request.getBranchGroup(), request.getIntermediateYear(), request.getSection(), request.getAcademicYear());
        }

        Student student = new Student();
        student.setStudentId(idGeneratorService.generateStudentId());
        student.setRollNumber(request.getRollNumber().trim());
        student.setAdmissionNumber(request.getAdmissionNumber());
        student.setFirstName(request.getFirstName().trim());
        student.setMiddleName(request.getMiddleName());
        student.setLastName(request.getLastName().trim());
        student.setFullName(request.getFullName());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setBloodGroup(request.getBloodGroup());
        student.setNationality(request.getNationality());
        student.setReligion(request.getReligion());
        student.setCasteCategory(request.getCasteCategory());
        student.setAadhaarNumber(request.getAadhaarNumber());
        student.setPanNumber(request.getPanNumber());
        student.setIdentificationMarks(request.getIdentificationMarks());
        student.setProfilePhotoUrl(request.getProfilePhotoUrl());
        student.setStatus(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE);

        if (createdByEmail != null) {
            userRepository.findByEmailIgnoreCase(createdByEmail).ifPresent(student::setCreatedBy);
        }

        // 1:1 Contact Detail
        StudentContactDetail contactDetail = new StudentContactDetail();
        contactDetail.setMobileNumber(request.getMobileNumber());
        contactDetail.setAlternateMobile(request.getAlternateMobile());
        contactDetail.setEmail(request.getEmail());
        contactDetail.setAddress(request.getAddress());
        contactDetail.setCity(request.getCity());
        contactDetail.setDistrict(request.getDistrict());
        contactDetail.setState(request.getState());
        contactDetail.setPinCode(request.getPinCode());
        contactDetail.setCountry(request.getCountry());
        student.setContactDetail(contactDetail);

        // 1:1 Parent Detail
        StudentParentDetail parentDetail = new StudentParentDetail();
        parentDetail.setFatherName(request.getFatherName());
        parentDetail.setMotherName(request.getMotherName());
        parentDetail.setParentMobile(request.getParentMobile());
        parentDetail.setParentEmail(request.getParentEmail());
        parentDetail.setOccupation(request.getOccupation());
        parentDetail.setAnnualIncome(request.getAnnualIncome());
        student.setParentDetail(parentDetail);

        // 1:1 Academic Detail
        StudentAcademicDetail academicDetail = new StudentAcademicDetail();
        academicDetail.setUniversityId(request.getUniversityId());
        academicDetail.setDepartment(request.getDepartment());
        academicDetail.setBranchGroup(request.getBranchGroup());
        academicDetail.setIntermediateYear(request.getIntermediateYear());
        academicDetail.setSemester(request.getSemester());
        academicDetail.setSection(request.getSection());
        academicDetail.setBatch(request.getBatch());
        academicDetail.setAcademicYear(request.getAcademicYear());
        academicDetail.setAdmissionDate(request.getAdmissionDate());
        academicDetail.setRegulation(request.getRegulation());
        academicDetail.setAdmissionType(request.getAdmissionType());
        academicDetail.setHostelDayScholar(request.getHostelDayScholar());
        academicDetail.setMedium(request.getMedium());
        student.setAcademicDetail(academicDetail);

        Student saved = studentRepository.save(student);
        return new StudentResponse(saved);
    }

        @Transactional(readOnly = true)
    public PaginatedStudentResponse<StudentSummaryResponse> getStudents(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String department,
            String academicYear,
            Integer currentYear,
            String section,
            StudentStatus status,
            String search,
            String currentUserEmail,
            boolean facultyScoped
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Student> studentPage;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            User user = userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null);
            Long userId = user != null ? user.getId() : -1L;
            studentPage = studentRepository.filterAndSearchStudentsForFaculty(
                faculty.getId(), userId, department, academicYear, currentYear, section, status, search, pageable
            );
        } else {
            studentPage = studentRepository.filterAndSearchStudents(
                department, academicYear, currentYear, section, status, search, pageable
            );
        }

        List<StudentSummaryResponse> content = studentPage.getContent().stream()
                .map(StudentSummaryResponse::new)
                .toList();

        return new PaginatedStudentResponse<StudentSummaryResponse>(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages(),
                studentPage.isLast()
        );
    }

        @Transactional(readOnly = true)
    public StudentResponse getStudentByPublicId(String studentId, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));
        return new StudentResponse(student);
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public StudentResponse updateStudent(String studentId, UpdateStudentRequest request, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        // Roll Number & Admission Number uniqueness check if updated
        if (request.getRollNumber() != null && !request.getRollNumber().trim().equalsIgnoreCase(student.getRollNumber())) {
            if (studentRepository.existsByRollNumberIgnoreCase(request.getRollNumber().trim())) {
                throw new DuplicateResourceException("Student with Roll Number '" + request.getRollNumber() + "' already exists.");
            }
            student.setRollNumber(request.getRollNumber().trim());
        }

        if (request.getAdmissionNumber() != null && !request.getAdmissionNumber().isBlank()
                && !request.getAdmissionNumber().trim().equalsIgnoreCase(student.getAdmissionNumber())) {
            if (studentRepository.existsByAdmissionNumberIgnoreCase(request.getAdmissionNumber().trim())) {
                throw new DuplicateResourceException("Student with Admission Number '" + request.getAdmissionNumber() + "' already exists.");
            }
            student.setAdmissionNumber(request.getAdmissionNumber().trim());
        }

        // Faculty Scope check if academic details change
        if (facultyScoped && (request.getBranchGroup() != null || request.getIntermediateYear() != null || request.getSection() != null || request.getAcademicYear() != null)) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            String g = request.getBranchGroup() != null ? request.getBranchGroup() : (student.getAcademicDetail() != null ? student.getAcademicDetail().getBranchGroup() : "");
            String y = request.getIntermediateYear() != null ? request.getIntermediateYear() : (student.getAcademicDetail() != null ? student.getAcademicDetail().getIntermediateYear() : "");
            String s = request.getSection() != null ? request.getSection() : (student.getAcademicDetail() != null ? student.getAcademicDetail().getSection() : "");
            String ay = request.getAcademicYear() != null ? request.getAcademicYear() : (student.getAcademicDetail() != null ? student.getAcademicDetail().getAcademicYear() : "");
            enforceFacultyAcademicScope(faculty, g, y, s, ay);
        }

        // Personal Information
        if (request.getFirstName() != null) student.setFirstName(request.getFirstName().trim());
        if (request.getMiddleName() != null) student.setMiddleName(request.getMiddleName().trim());
        if (request.getLastName() != null) student.setLastName(request.getLastName().trim());
        if (request.getFullName() != null) student.setFullName(request.getFullName().trim());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        if (request.getBloodGroup() != null) student.setBloodGroup(request.getBloodGroup());
        if (request.getNationality() != null) student.setNationality(request.getNationality());
        if (request.getReligion() != null) student.setReligion(request.getReligion());
        if (request.getCasteCategory() != null) student.setCasteCategory(request.getCasteCategory());
        if (request.getAadhaarNumber() != null) student.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getPanNumber() != null) student.setPanNumber(request.getPanNumber());
        if (request.getIdentificationMarks() != null) student.setIdentificationMarks(request.getIdentificationMarks());
        if (request.getProfilePhotoUrl() != null) student.setProfilePhotoUrl(request.getProfilePhotoUrl());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        // Contact Detail
        StudentContactDetail contact = student.getContactDetail();
        if (contact == null) {
            contact = new StudentContactDetail();
            contact.setStudent(student);
            student.setContactDetail(contact);
        }
        if (request.getMobileNumber() != null) contact.setMobileNumber(request.getMobileNumber());
        if (request.getAlternateMobile() != null) contact.setAlternateMobile(request.getAlternateMobile());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getAddress() != null) contact.setAddress(request.getAddress());
        if (request.getCity() != null) contact.setCity(request.getCity());
        if (request.getDistrict() != null) contact.setDistrict(request.getDistrict());
        if (request.getState() != null) contact.setState(request.getState());
        if (request.getPinCode() != null) contact.setPinCode(request.getPinCode());
        if (request.getCountry() != null) contact.setCountry(request.getCountry());

        // Parent Detail
        StudentParentDetail parent = student.getParentDetail();
        if (parent == null) {
            parent = new StudentParentDetail();
            parent.setStudent(student);
            student.setParentDetail(parent);
        }
        if (request.getFatherName() != null) parent.setFatherName(request.getFatherName());
        if (request.getMotherName() != null) parent.setMotherName(request.getMotherName());
        if (request.getParentMobile() != null) parent.setParentMobile(request.getParentMobile());
        if (request.getParentEmail() != null) parent.setParentEmail(request.getParentEmail());
        if (request.getOccupation() != null) parent.setOccupation(request.getOccupation());
        if (request.getAnnualIncome() != null) parent.setAnnualIncome(request.getAnnualIncome());

        // Academic Detail
        StudentAcademicDetail academic = student.getAcademicDetail();
        if (academic == null) {
            academic = new StudentAcademicDetail();
            academic.setStudent(student);
            student.setAcademicDetail(academic);
        }
        if (request.getUniversityId() != null) academic.setUniversityId(request.getUniversityId());
        if (request.getDepartment() != null) academic.setDepartment(request.getDepartment());
        if (request.getBranchGroup() != null) academic.setBranchGroup(request.getBranchGroup());
        if (request.getIntermediateYear() != null) academic.setIntermediateYear(request.getIntermediateYear());
        if (request.getSemester() != null) academic.setSemester(request.getSemester());
        if (request.getSection() != null) academic.setSection(request.getSection());
        if (request.getBatch() != null) academic.setBatch(request.getBatch());
        if (request.getAcademicYear() != null) academic.setAcademicYear(request.getAcademicYear());
        if (request.getAdmissionDate() != null) academic.setAdmissionDate(request.getAdmissionDate());
        if (request.getRegulation() != null) academic.setRegulation(request.getRegulation());
        if (request.getAdmissionType() != null) academic.setAdmissionType(request.getAdmissionType());
        if (request.getHostelDayScholar() != null) academic.setHostelDayScholar(request.getHostelDayScholar());
        if (request.getMedium() != null) academic.setMedium(request.getMedium());

        Student saved = studentRepository.save(student);
        return new StudentResponse(saved);
    }


    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public void deactivateStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResponse> searchStudents(String query) {
        return searchStudents(query, null, false);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResponse> searchStudents(String query, String currentUserEmail, boolean facultyScoped) {
        String cleanQuery = (query == null) ? "" : query.trim();
        List<Student> results;
        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            User user = userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null);
            Long userId = user != null ? user.getId() : -1L;
            results = studentRepository.searchByQueryForFaculty(cleanQuery, faculty.getId(), userId);
        } else {
            results = studentRepository.searchByQuery(cleanQuery);
        }
        return results.stream().map(StudentSearchResponse::new).toList();
    }

    @Transactional
    public void updatePhotoUrl(String studentId, String photoUrl, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        student.setProfilePhotoUrl(photoUrl);
        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public StudentIdCardResponse getStudentIdCard(String studentId, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        String qrPayload = qrService.generateVerificationUrl(student.getStudentId());
        return new StudentIdCardResponse(student, qrPayload);
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public void deleteStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }

        Student student = studentRepository.findByStudentId(studentId.trim()).orElse(null);
        if (student == null) {
            try {
                student = studentRepository.findById(Long.parseLong(studentId.trim())).orElse(null);
            } catch (NumberFormatException ignored) {}
        }

        if (student == null) {
            System.out.println(">>> STUDENT ALREADY PURGED OR NOT FOUND: " + studentId);
            return;
        }

        String actualStudentId = student.getStudentId();

        // 1. Delete all student documents and versions safely
        List<StudentDocument> documents = documentRepository.findByStudent_StudentId(actualStudentId);
        if (documents.isEmpty()) {
            documents = documentRepository.findByStudentId(student.getId());
        }

        for (StudentDocument doc : documents) {
            List<DocumentVersion> versions =
                    documentVersionRepository.findByStudentDocumentIdOrderByVersionNumberDesc(doc.getId());
            for (DocumentVersion version : versions) {
                if (version.getStoragePath() != null && !version.getStoragePath().isBlank()) {
                    try {
                        documentStorageService.deleteFile(version.getStoragePath());
                    } catch (Exception ignored) {}
                }
            }
            documentVersionRepository.deleteAll(versions);

            if (doc.getStoragePath() != null && !doc.getStoragePath().isBlank()) {
                try {
                    documentStorageService.deleteFile(doc.getStoragePath());
                } catch (Exception ignored) {}
            }
            documentRepository.delete(doc);
        }

        // 2. Delete student profile photo from Supabase Storage safely
        String profilePhotoUrl = student.getProfilePhotoUrl();
        if (profilePhotoUrl != null && !profilePhotoUrl.isBlank()) {
            try {
                photoService.deletePhotoFile(profilePhotoUrl);
            } catch (Exception ignored) {}
        }

        // 3. Delete student database record
        studentRepository.delete(student);

        // 4. Safely disable linked user account if present without breaking transaction
        if (student.getContactDetail() != null && student.getContactDetail().getEmail() != null) {
            String email = student.getContactDetail().getEmail();
            try {
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    user.setAccountEnabled(false);
                    userRepository.save(user);
                });
            } catch (Exception ignored) {}
        }

        System.out.println(">>> STUDENT PURGED SUCCESSFULLY: " + actualStudentId + " (id=" + student.getId() + ")");
    }

    private Optional<Student> loadStudentForCurrentUser(String studentId, String currentUserEmail, boolean facultyScoped) {
        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null) {
            return Optional.empty();
        }

        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            if (!facultyService.hasAccessToStudent(faculty, student)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty can only access students from assigned sections.");
            }
        }

        return Optional.of(student);
    }

    private void enforceFacultyAcademicScope(Faculty faculty, String branchGroup, String intermediateYear, String section, String academicYear) {
        if (!facultyService.hasAccessToAcademicScope(faculty, branchGroup, intermediateYear, section, academicYear)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty can only create students in assigned sections.");
        }
    }

    @Transactional(readOnly = true)
    public String determineExportFilename(String currentUserEmail, boolean isFaculty) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        if (!isFaculty) {
            return "All_Students_" + today + ".xlsx";
        }

        Faculty faculty = facultyService.findFacultyByUserEmail(currentUserEmail).orElse(null);
        if (faculty == null) {
            return "Assigned_Students.xlsx";
        }

        List<String> sectionNames = facultyService.getFacultyAssignedSectionFormattedNames(faculty.getId());
        if (sectionNames == null || sectionNames.isEmpty()) {
            return "Assigned_Students.xlsx";
        }

        String rawName = String.join("_", sectionNames) + "_Students.xlsx";
        return rawName.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /**
     * Export permitted students directly to output stream as Excel (.xlsx) with Audit Logging.
     */
    @Transactional
    public void exportStudentsToExcel(OutputStream outputStream, String currentUserEmail, boolean isFaculty, String ipAddress) throws IOException {
        List<Student> students;
        String sectionNamesStr = "ALL";
        Long userId = null;
        String role = isFaculty ? "ROLE_FACULTY" : "ROLE_ADMIN";

        User currentUser = currentUserEmail != null ? userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null) : null;
        if (currentUser != null) {
            userId = currentUser.getId();
            if (currentUser.getRole() != null && currentUser.getRole().getRoleName() != null) {
                role = currentUser.getRole().getRoleName();
            }
        }

        if (isFaculty) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            Long facultyUserId = currentUser != null ? currentUser.getId() : -1L;
            students = studentRepository.findAccessibleStudentsForFacultyExport(faculty.getId(), facultyUserId);
            List<String> assignedSections = facultyService.getFacultyAssignedSectionFormattedNames(faculty.getId());
            sectionNamesStr = (assignedSections != null && !assignedSections.isEmpty()) ? String.join(", ", assignedSections) : "Self-Created";
        } else {
            students = studentRepository.findAllForExcelExport();
        }

        List<StudentDocument> allDocs = documentRepository.findAllWithStudentAndType();
        Map<String, List<StudentDocument>> studentDocsMap = allDocs.stream()
                .filter(d -> d.getStudent() != null && d.getStudent().getStudentId() != null)
                .collect(Collectors.groupingBy(d -> d.getStudent().getStudentId()));
        List<DocumentType> requiredTypes = documentTypeRepository.findByActiveTrue();

        StudentExcelExporter.exportToStream(students, studentDocsMap, requiredTypes, outputStream);

        // Audit Logging
        try {
            ExportAuditLog auditLog = new ExportAuditLog(
                    userId,
                    currentUserEmail,
                    role,
                    sectionNamesStr,
                    students.size(),
                    ipAddress
            );
            exportAuditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to save export audit log: " + e.getMessage());
        }
    }
}
