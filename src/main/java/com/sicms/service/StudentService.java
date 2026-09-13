package com.sicms.service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import com.sicms.entity.User;
import com.sicms.entity.DocumentType;
import com.sicms.entity.ExportAuditLog;
import com.sicms.entity.AcademicGroup;
import com.sicms.repository.AcademicGroupRepository;
import com.sicms.util.StudentExcelExporter;

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
import com.sicms.entity.Faculty;
import com.sicms.entity.Student;
import com.sicms.entity.StudentDocument;
import com.sicms.entity.StudentStatus;
import com.sicms.exception.DuplicateResourceException;
import com.sicms.exception.StudentNotFoundException;
import com.sicms.repository.DocumentTypeRepository;
import com.sicms.repository.StudentDocumentRepository;
import com.sicms.repository.ExportAuditLogRepository;
import com.sicms.repository.StudentRepository;
import com.sicms.repository.UserRepository;
import com.sicms.repository.DocumentVersionRepository;
import com.sicms.repository.RefreshTokenRepository;
import com.sicms.repository.PasswordResetOtpRepository;
import com.sicms.repository.OtpRepository;
import com.sicms.entity.DocumentVersion;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final com.sicms.repository.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final StudentIdGeneratorService idGeneratorService;
    private final StudentQrService qrService;
    private final StudentDocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentStorageService documentStorageService;
    private final FacultyService facultyService;
    private final StudentPhotoService photoService;
    private final DocumentTypeRepository documentTypeRepository;
    private final ExportAuditLogRepository exportAuditLogRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final OtpRepository otpRepository;

    @Autowired
    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            com.sicms.repository.RoleRepository roleRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            StudentIdGeneratorService idGeneratorService,
            StudentQrService qrService,
            StudentDocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentStorageService documentStorageService,
            FacultyService facultyService,
            StudentPhotoService photoService,
            DocumentTypeRepository documentTypeRepository,
            ExportAuditLogRepository exportAuditLogRepository,
            AcademicGroupRepository academicGroupRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            OtpRepository otpRepository
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGeneratorService = idGeneratorService;
        this.qrService = qrService;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentStorageService = documentStorageService;
        this.facultyService = facultyService;
        this.photoService = photoService;
        this.documentTypeRepository = documentTypeRepository;
        this.exportAuditLogRepository = exportAuditLogRepository;
        this.academicGroupRepository = academicGroupRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.otpRepository = otpRepository;
    }

    public static String formatDobPassword(LocalDate dob) {
        if (dob == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return dob.format(formatter);
    }

    public void createStudentUserAccount(Student student) {
        if (student == null || student.getEmailAddress1() == null || student.getEmailAddress1().isBlank()) {
            return;
        }
        String primaryEmail = student.getEmailAddress1().trim().toLowerCase();
        com.sicms.entity.Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.findByRoleName("STUDENT")
                .orElseGet(() -> roleRepository.save(new com.sicms.entity.Role("ROLE_STUDENT", "Student User Role"))));

        java.util.Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(primaryEmail);
        if (existingUserOpt.isPresent()) {
            // User account already exists: preserve passwordHash and mustChangePassword!
            User existingUser = existingUserOpt.get();
            boolean updated = false;
            if (existingUser.getStudent() == null) {
                existingUser.setStudent(student);
                updated = true;
            }
            if (student.getFullName() != null && !student.getFullName().isBlank() &&
                    (existingUser.getFullName() == null || existingUser.getFullName().isBlank())) {
                existingUser.setFullName(student.getFullName().trim());
                updated = true;
            }
            if (updated) {
                userRepository.save(existingUser);
            }
            return;
        }

        String defaultPassword = formatDobPassword(student.getDateOfBirth());
        if (defaultPassword == null || defaultPassword.isBlank()) {
            defaultPassword = "01-01-2000";
        }

        User studentUser = new User();
        studentUser.setFullName(student.getFullName());
        studentUser.setEmail(primaryEmail);
        studentUser.setPasswordHash(passwordEncoder.encode(defaultPassword));
        studentUser.setRole(studentRole);
        studentUser.setAuthProvider(com.sicms.entity.AuthProvider.LOCAL);
        studentUser.setEmailVerified(true);
        studentUser.setAccountEnabled(true);
        studentUser.setMustChangePassword(true);
        studentUser.setStudent(student);

        userRepository.save(studentUser);
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, String createdByEmail, boolean facultyScoped) {
        if (request.getEmailAddress1() == null || request.getEmailAddress1().isBlank()) {
            throw new IllegalArgumentException("Primary email address is required to create the Student Portal account.");
        }
        String primaryEmail = request.getEmailAddress1().trim().toLowerCase();

        if (request.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required to create the default password for the Student Portal account.");
        }

        if (userRepository.findByEmailIgnoreCase(primaryEmail).isPresent()) {
            throw new DuplicateResourceException("This email address is already registered with another account.");
        }

        if (request.getAdmissionNumber() != null && !request.getAdmissionNumber().isBlank()
                && studentRepository.existsByAdmissionNumberIgnoreCase(request.getAdmissionNumber().trim())) {
            throw new DuplicateResourceException("Student with Admission Number '" + request.getAdmissionNumber() + "' already exists.");
        }

        String studentId = request.getStudentId();
        if (studentId != null && !studentId.isBlank()) {
            if (studentRepository.existsByStudentId(studentId.trim())) {
                throw new DuplicateResourceException("Student with ID '" + studentId + "' already exists.");
            }
            studentId = studentId.trim();
        } else {
            studentId = idGeneratorService.generateStudentId();
        }

        if (facultyScoped) {
            Faculty faculty = facultyService.getFacultyByUserEmail(createdByEmail);
            enforceFacultyAcademicScope(faculty, request.getBranchGroup(), request.getIntermediateYear(), request.getSection(), request.getAcademicYear());
        }

        Student student = new Student();
        student.setStudentId(studentId);
        student.setAdmissionNumber(request.getAdmissionNumber() != null && !request.getAdmissionNumber().isBlank() ? request.getAdmissionNumber().trim() : null);
        student.setFullName(request.getFullName().trim());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setNationality(request.getNationality() != null ? request.getNationality() : "Indian");
        student.setReligion(request.getReligion());
        student.setCategory(request.getCategory());
        student.setAadhaarNumber(request.getAadhaarNumber());
        student.setProfilePhotoUrl(request.getProfilePhotoUrl());
        student.setMobileNumber(request.getMobileNumber());
        student.setAlternateMobile(request.getAlternateMobile());
        student.setEmailAddress1(primaryEmail);
        student.setEmailAddress2(request.getEmailAddress2() != null ? request.getEmailAddress2().trim() : null);
        student.setFatherName(request.getFatherName() != null ? request.getFatherName().trim() : null);
        student.setMotherName(request.getMotherName() != null ? request.getMotherName().trim() : null);
        student.setAcademicYear(request.getAcademicYear() != null ? request.getAcademicYear().trim() : null);
        student.setBranchGroup(request.getBranchGroup() != null ? request.getBranchGroup().trim() : null);
        student.setIntermediateYear(request.getIntermediateYear() != null ? request.getIntermediateYear().trim() : null);
        student.setBatch(request.getBatch() != null ? request.getBatch().trim() : null);
        student.setAdmissionType(request.getAdmissionType() != null ? request.getAdmissionType() : "REGULAR");
        student.setHostelDayScholar(request.getHostelDayScholar() != null ? request.getHostelDayScholar() : "DAY_SCHOLAR");
        if (request.getCampus() != null) {
            student.setCampus(request.getCampus().trim());
        }

        student.setSection(request.getSection() != null && !request.getSection().isBlank() ? request.getSection().trim() : "Unassigned");
        student.setStatus(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE);

        if (createdByEmail != null) {
            userRepository.findByEmailIgnoreCase(createdByEmail).ifPresent(student::setCreatedBy);
        }

        Student saved = studentRepository.save(student);

        // Automatically Create Student Portal User Account
        createStudentUserAccount(saved);

        return new StudentResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedStudentResponse<StudentSummaryResponse> getStudents(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String campus,
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
                campus, department, academicYear, currentYear, section, status, search, pageable
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
    public List<String> getDistinctGroups() {
        Set<String> uniqueGroups = new LinkedHashSet<>();
        try {
            List<String> rawGroups = studentRepository.findDistinctBranchGroups();
            if (rawGroups != null) {
                for (String g : rawGroups) {
                    if (g != null && !g.trim().isBlank()) {
                        uniqueGroups.add(g.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<AcademicGroup> agList = academicGroupRepository.findAll();
            if (agList != null) {
                for (AcademicGroup ag : agList) {
                    if (ag.getCode() != null && !ag.getCode().trim().isBlank()) {
                        uniqueGroups.add(ag.getCode().trim().toUpperCase());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new ArrayList<>(uniqueGroups);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByPublicId(String studentId, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));
        return new StudentResponse(student);
    }

    @Transactional
    public Student getStudentEntityByEmailOrUserId(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new StudentNotFoundException("Student email or user identifier is required.");
        }
        String cleanId = identifier.trim();

        // 1. Try to find User by email/identifier first
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStudent() != null) {
                return user.getStudent();
            }
        }

        // 2. Extract prefix if identifier is a composite email (e.g. STU2026001001@student.bhashyam.edu or STU2026001001)
        String prefix = cleanId;
        if (cleanId.contains("@")) {
            prefix = cleanId.substring(0, cleanId.indexOf("@")).trim();
        }

        // 3. Search Student repository by email, studentId (or extracted prefix), or admissionNumber
        Optional<Student> studentOpt = studentRepository.findByEmailOrStudentId(cleanId);
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByEmailOrStudentId(prefix);
        }
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByStudentIdIgnoreCase(prefix);
        }
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByAdmissionNumberIgnoreCase(prefix);
        }

        Student student = studentOpt.orElseThrow(() ->
                new StudentNotFoundException("No student record linked to account '" + identifier + "'."));

        // 4. Auto-link Student to User in DB if missing so future lookups are instant
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStudent() == null) {
                user.setStudent(student);
                userRepository.save(user);
            }
        } else if (student.getEmailAddress1() != null && !student.getEmailAddress1().isBlank()) {
            userRepository.findByEmailIgnoreCase(student.getEmailAddress1().trim()).ifPresent(u -> {
                if (u.getStudent() == null) {
                    u.setStudent(student);
                    userRepository.save(u);
                }
            });
        }

        return student;
    }

    @Transactional
    public StudentResponse getStudentByEmailOrUserId(String identifier) {
        Student student = getStudentEntityByEmailOrUserId(identifier);
        return new StudentResponse(student);
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public StudentResponse updateStudent(String studentId, UpdateStudentRequest request, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        if (request.getAdmissionNumber() != null && !request.getAdmissionNumber().isBlank()
                && !request.getAdmissionNumber().trim().equalsIgnoreCase(student.getAdmissionNumber())) {
            if (studentRepository.existsByAdmissionNumberIgnoreCase(request.getAdmissionNumber().trim())) {
                throw new DuplicateResourceException("Student with Admission Number '" + request.getAdmissionNumber() + "' already exists.");
            }
            student.setAdmissionNumber(request.getAdmissionNumber().trim());
        }

        if (facultyScoped && (request.getBranchGroup() != null || request.getIntermediateYear() != null || request.getSection() != null || request.getAcademicYear() != null)) {
            Faculty faculty = facultyService.getFacultyByUserEmail(currentUserEmail);
            String g = request.getBranchGroup() != null ? request.getBranchGroup() : student.getBranchGroup();
            String y = request.getIntermediateYear() != null ? request.getIntermediateYear() : student.getIntermediateYear();
            String s = request.getSection() != null ? request.getSection() : student.getSection();
            String ay = request.getAcademicYear() != null ? request.getAcademicYear() : student.getAcademicYear();
            enforceFacultyAcademicScope(faculty, g, y, s, ay);
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) student.setFullName(request.getFullName().trim());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        if (request.getNationality() != null) student.setNationality(request.getNationality());
        if (request.getReligion() != null) student.setReligion(request.getReligion());
        if (request.getCategory() != null) student.setCategory(request.getCategory());
        if (request.getAadhaarNumber() != null) student.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getMobileNumber() != null) student.setMobileNumber(request.getMobileNumber());
        if (request.getAlternateMobile() != null) student.setAlternateMobile(request.getAlternateMobile());
        if (request.getEmailAddress1() != null) student.setEmailAddress1(request.getEmailAddress1().trim());
        if (request.getEmailAddress2() != null) student.setEmailAddress2(request.getEmailAddress2().trim());
        if (request.getFatherName() != null) student.setFatherName(request.getFatherName().trim());
        if (request.getMotherName() != null) student.setMotherName(request.getMotherName().trim());
        if (request.getAcademicYear() != null) student.setAcademicYear(request.getAcademicYear().trim());
        if (request.getBranchGroup() != null) student.setBranchGroup(request.getBranchGroup().trim());
        if (request.getIntermediateYear() != null) student.setIntermediateYear(request.getIntermediateYear().trim());
        if (request.getBatch() != null) student.setBatch(request.getBatch().trim());
        if (request.getAdmissionType() != null) student.setAdmissionType(request.getAdmissionType());
        if (request.getHostelDayScholar() != null) student.setHostelDayScholar(request.getHostelDayScholar());
        if (request.getSection() != null) student.setSection(request.getSection().trim());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        if (request.getProfilePhotoUrl() != null) {
            String newPhoto = request.getProfilePhotoUrl().trim();
            String oldPhoto = student.getProfilePhotoUrl();
            if (newPhoto.isBlank() || "null".equalsIgnoreCase(newPhoto)) {
                student.setProfilePhotoUrl(null);
                if (oldPhoto != null && !oldPhoto.isBlank()) {
                    try {
                        photoService.deletePhotoFile(oldPhoto);
                    } catch (Exception ignored) {}
                }
            } else {
                student.setProfilePhotoUrl(newPhoto);
                if (oldPhoto != null && !oldPhoto.isBlank() && !oldPhoto.equals(newPhoto)) {
                    try {
                        photoService.deletePhotoFile(oldPhoto);
                    } catch (Exception ignored) {}
                }
            }
        }

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

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public void updatePhotoUrl(String studentId, String photoUrl, String currentUserEmail, boolean facultyScoped) {
        Student student = loadStudentForCurrentUser(studentId, currentUserEmail, facultyScoped)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID '" + studentId + "' not found."));

        String oldPhoto = student.getProfilePhotoUrl();
        student.setProfilePhotoUrl(photoUrl);
        studentRepository.save(student);

        if (oldPhoto != null && !oldPhoto.isBlank() && !oldPhoto.equals(photoUrl)) {
            try {
                photoService.deletePhotoFile(oldPhoto);
            } catch (Exception ignored) {}
        }
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
    public boolean deleteStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new StudentNotFoundException("Student ID must not be blank.");
        }

        String clean = studentId.trim();
        Student student = studentRepository.findByStudentId(clean).orElse(null);
        if (student == null) {
            student = studentRepository.findByStudentIdIgnoreCase(clean).orElse(null);
        }
        if (student == null) {
            try {
                student = studentRepository.findById(Long.parseLong(clean)).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (student == null) {
            student = studentRepository.findByAdmissionNumberIgnoreCase(clean).orElse(null);
        }

        if (student == null) {
            System.out.println(">>> [DELETE STUDENT] Student record not found for query: " + studentId);
            throw new StudentNotFoundException("Student not found with ID: " + studentId);
        }

        Long studentDbId = student.getId();
        String actualStudentId = student.getStudentId();
        System.out.println(">>> [DELETE STUDENT] Deleting student id=" + studentDbId + " (" + actualStudentId + ")");

        // 1. Fetch linked user accounts via the database relationship (User.student = student)
        List<User> linkedUsers = userRepository.findByStudent(student);

        // 2. Delete all student documents and versions safely (Supabase storage + DB records)
        List<StudentDocument> documents = documentRepository.findByStudent_StudentId(actualStudentId);
        if (documents.isEmpty()) {
            documents = documentRepository.findByStudentId(studentDbId);
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

        // 3. Delete student profile photo from Supabase Storage safely
        String profilePhotoUrl = student.getProfilePhotoUrl();
        if (profilePhotoUrl != null && !profilePhotoUrl.isBlank()) {
            try {
                photoService.deletePhotoFile(profilePhotoUrl);
            } catch (Exception ignored) {}
        }

        // 4. In order to delete the student without foreign key violation from users.student_id (ON DELETE NO ACTION),
        // unlink the student reference on each linked user within this transaction before student record deletion.
        if (linkedUsers != null && !linkedUsers.isEmpty()) {
            for (User u : linkedUsers) {
                u.setStudent(null);
                userRepository.save(u);
            }
            userRepository.flush();
        }

        // 5. Delete student record from students table
        studentRepository.delete(student);
        studentRepository.flush();

        // 6. Delete linked user accounts and clean their foreign key dependencies
        if (linkedUsers != null && !linkedUsers.isEmpty()) {
            for (User u : linkedUsers) {
                Long userId = u.getId();
                System.out.println(">>> [DELETE STUDENT] Deleting linked user id=" + userId + " for student id=" + studentDbId);

                // Clear user foreign key references in other tables
                studentRepository.clearCreatedByForUser(userId);
                exportAuditLogRepository.clearUserReferences(userId);
                refreshTokenRepository.deleteByUser(u);
                passwordResetOtpRepository.deleteByUser(u);
                otpRepository.deleteByUser(u);

                // Permanently delete user record
                userRepository.delete(u);
                userRepository.flush();
                System.out.println(">>> [DELETE STUDENT] Linked user id=" + userId + " deleted successfully");
            }
        } else {
            System.out.println(">>> [DELETE STUDENT] No linked user account found for student id=" + studentDbId + " (legacy/imported student)");
        }

        System.out.println(">>> [DELETE STUDENT] Student id=" + studentDbId + " (" + actualStudentId + ") deleted successfully");
        return true;
    }

    @CacheEvict(value = {"adminDashboard", "facultyDashboard", "studentProfile", "students", "studentSummaries"}, allEntries = true)
    @Transactional
    public int deleteStudentsBulk(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0;
        }

        List<String> cleanIds = studentIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (cleanIds.isEmpty()) {
            return 0;
        }

        System.out.println(">>> [BULK DELETE SERVICE] Processing " + cleanIds.size() + " student IDs: " + cleanIds);
        int deletedCount = 0;
        for (String id : cleanIds) {
            try {
                boolean success = deleteStudent(id);
                if (success) {
                    deletedCount++;
                }
            } catch (StudentNotFoundException e) {
                System.out.println(">>> [BULK DELETE SERVICE] Student not found for ID: " + id + ", skipping.");
            }
        }
        System.out.println(">>> [BULK DELETE SERVICE] Successfully deleted " + deletedCount + " out of " + cleanIds.size() + " students.");
        return deletedCount;
    }

    public Optional<Student> loadStudentForCurrentUser(String studentId, String currentUserEmail, boolean facultyScoped) {
        if (studentId == null || studentId.isBlank()) {
            return Optional.empty();
        }
        String clean = studentId.trim();
        Student student = studentRepository.findByStudentId(clean).orElse(null);
        if (student == null) {
            try {
                Long idNum = Long.parseLong(clean);
                student = studentRepository.findById(idNum).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (student == null) {
            student = studentRepository.findByAdmissionNumberIgnoreCase(clean).orElse(null);
        }
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
        if (sectionNames.isEmpty()) {
            return "Assigned_Students_" + today + ".xlsx";
        }

        String sanitized = String.join("_", sectionNames).replaceAll("[^a-zA-Z0-9_-]", "_");
        return "Students_" + sanitized + "_" + today + ".xlsx";
    }

    @Transactional(readOnly = true)
    public void exportStudentsToExcel(OutputStream outputStream, String currentUserEmail, boolean isFaculty, String clientIp) throws IOException {
        List<Student> students;
        if (!isFaculty) {
            students = studentRepository.findAllForExcelExport();
        } else {
            Faculty faculty = facultyService.findFacultyByUserEmail(currentUserEmail).orElse(null);
            if (faculty != null) {
                User user = userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null);
                Long userId = user != null ? user.getId() : -1L;
                students = studentRepository.findAccessibleStudentsForFacultyExport(faculty.getId(), userId);
            } else {
                students = List.of();
            }
        }

        List<DocumentType> requiredTypes = documentTypeRepository.findByActiveTrue();
        Map<String, List<StudentDocument>> docsMap = Map.of();

        StudentExcelExporter.exportToStream(students, docsMap, requiredTypes, outputStream);

        try {
            User user = currentUserEmail != null ? userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null) : null;
            ExportAuditLog log = new ExportAuditLog(
                user != null ? user.getId() : null,
                currentUserEmail,
                isFaculty ? "ROLE_FACULTY" : "ROLE_ADMIN",
                "EXPORT_EXCEL",
                students.size(),
                clientIp != null ? clientIp : "UNKNOWN"
            );
            exportAuditLogRepository.save(log);
        } catch (Exception ignored) {}
    }
}
