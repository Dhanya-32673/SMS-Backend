package com.sicms.config;

import com.sicms.entity.*;
import com.sicms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final AcademicGroupRepository groupRepository;
    private final AcademicSectionRepository sectionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final StudentRepository studentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final javax.sql.DataSource dataSource;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           FacultyRepository facultyRepository,
                           FacultyAssignmentRepository assignmentRepository,
                           AcademicGroupRepository groupRepository,
                           AcademicSectionRepository sectionRepository,
                           DocumentTypeRepository documentTypeRepository,
                           StudentRepository studentRepository,
                           StudentDocumentRepository studentDocumentRepository,
                           PermissionRepository permissionRepository,
                           PasswordEncoder passwordEncoder,
                           javax.sql.DataSource dataSource) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.assignmentRepository = assignmentRepository;
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.studentRepository = studentRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            System.out.println("=================================================");
            System.out.println(">>> SICMS CONNECTED DATABASE: " + conn.getMetaData().getURL());
            System.out.println(">>> DATABASE PRODUCT: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println(">>> DATABASE USER: " + conn.getMetaData().getUserName());
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println(">>> FAILED TO QUERY DATABASE METADATA: " + e.getMessage());
        }

        System.out.println("=================================================");
        System.out.println(">>> SICMS: INITIALIZING LINKED ORIGINAL DATA...");
        System.out.println("=================================================");

        // === PHASE 1: Roles (CRITICAL - must succeed) ===
        Role adminRole;
        Role facultyRole;
        try {
            adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator")));
            facultyRole = roleRepository.findByRoleName("ROLE_FACULTY")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_FACULTY", "Faculty Member")));
            System.out.println(">>> [Phase 1] Roles OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 1] Roles FAILED (cannot continue): " + e.getMessage());
            return;
        }

        // === PHASE 2: Permissions ===
        try {
            seedPermission("STUDENT_VIEW", "View Students", "STUDENT");
            seedPermission("STUDENT_CREATE", "Create Student", "STUDENT");
            seedPermission("STUDENT_EDIT", "Edit Student", "STUDENT");
            seedPermission("STUDENT_DEACTIVATE", "Deactivate Student", "STUDENT");
            seedPermission("CERTIFICATE_VIEW", "View Certificates", "CERTIFICATE");
            seedPermission("CERTIFICATE_UPLOAD", "Upload Certificate", "CERTIFICATE");
            seedPermission("CERTIFICATE_VERIFY", "Verify Certificate", "CERTIFICATE");
            seedPermission("CERTIFICATE_REJECT", "Reject Certificate", "CERTIFICATE");
            seedPermission("FACULTY_VIEW", "View Faculty", "FACULTY");
            seedPermission("FACULTY_CREATE", "Create Faculty", "FACULTY");
            seedPermission("FACULTY_EDIT", "Edit Faculty", "FACULTY");
            seedPermission("FACULTY_ASSIGN", "Assign Faculty", "FACULTY");
            seedPermission("GROUP_MANAGE", "Manage Academic Groups", "ACADEMIC");
            seedPermission("SECTION_MANAGE", "Manage Sections", "ACADEMIC");
            System.out.println(">>> [Phase 2] Permissions OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 2] Permissions skipped: " + e.getMessage());
        }

        // === PHASE 3: Admin User (CRITICAL) ===
        try {
            seedUser("admin@college.edu", "System Administrator", "AdminPass123!", adminRole);
            seedUser("dhanyaande@gmail.com", "Dhanya Aande (Admin)", "AdminPass123!", adminRole);
            System.out.println(">>> [Phase 3] Admin users OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 3] Admin users skipped: " + e.getMessage());
        }

        // === PHASE 4: Academic Groups ===
        try {
            seedGroup("MPC", "Maths, Physics, Chemistry", "Intermediate Science Stream");
            seedGroup("BiPC", "Biology, Physics, Chemistry", "Intermediate Biology Stream");
            seedGroup("MEC", "Maths, Economics, Commerce", "Intermediate Commerce/Maths Stream");
            seedGroup("CEC", "Civics, Economics, Commerce", "Intermediate Commerce Stream");
            seedGroup("HEC", "History, Economics, Civics", "Intermediate Arts Stream");
            System.out.println(">>> [Phase 4] Academic Groups OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 4] Academic Groups skipped: " + e.getMessage());
        }

        // === PHASE 5: Academic Sections ===
        try {
            seedSection("A", "MPC", "1st Year", "2026-2027", 60);
            seedSection("B", "MPC", "1st Year", "2026-2027", 60);
            seedSection("A", "MPC", "2nd Year", "2026-2027", 60);
            seedSection("A", "BiPC", "1st Year", "2026-2027", 60);
            seedSection("B", "MEC", "1st Year", "2026-2027", 60);
            seedSection("A", "CEC", "2nd Year", "2026-2027", 60);
            seedSection("A", "HEC", "1st Year", "2026-2027", 60);
            System.out.println(">>> [Phase 5] Academic Sections OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 5] Academic Sections skipped: " + e.getMessage());
        }

        // === PHASE 6: Document Types ===
        try {
            seedDocumentType("SSC_MEMO", "SSC / 10th Marks Memo", "ACADEMIC", "10th class secondary school certificate memo", true, false);
            seedDocumentType("INTER_1ST_MEMO", "Intermediate 1st Year Memo", "ACADEMIC", "Junior Intermediate marks memo", true, false);
            seedDocumentType("TRANSFER_CERT", "Transfer Certificate (TC)", "ADMISSION", "Official institution transfer certificate", true, false);
            seedDocumentType("AADHAAR_DOC", "Aadhaar Card Document", "IDENTITY", "Government issued Aadhaar identity card scan", true, false);
            System.out.println(">>> [Phase 6] Document Types OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 6] Document Types skipped: " + e.getMessage());
        }

        // === PHASE 7: Faculty & Assignments (CRITICAL) ===
        Faculty f5 = null;
        try {
            f5 = seedFaculty("2400032673cse1@gmail.com", "Faculty Member", "FacultyPass123!", facultyRole, "FAC1005", "EMP-202605", "Faculty", "Member", "MALE", "9849055555", "Lecturer", "M.Sc.", "General Sciences", "HEC");
            seedAssignment(f5, "HEC", "1st Year", "A", "2026-2027", "History & Social Studies");
            seedAssignment(f5, "MPC", "1st Year", "A", "2026-2027", "Physics & Mathematics");
            System.out.println(">>> [Phase 7] Faculty & Assignments OK");
        } catch (Exception e) {
            System.err.println(">>> [Phase 7] Faculty skipped: " + e.getMessage());
        }

        // === PHASE 8: Students (REMOVED - No automatic student insertion) ===
        System.out.println(">>> [Phase 8] Students automatic mock insertion: DISABLED");

        // === PHASE 9: Student Documents (REMOVED - No automatic certificate insertion) ===
        System.out.println(">>> [Phase 9] Student Documents automatic mock insertion: DISABLED");

        System.out.println("=================================================");
        System.out.println(">>> SICMS: STARTUP COMPLETE — SUPABASE POSTGRESQL CONNECTED!");
        System.out.println("=================================================");
    }

    private User seedUser(String email, String fullName, String rawPassword, Role role) {
        return userRepository.findByEmailIgnoreCase(email).map(existingUser -> {
            if (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isBlank()) {
                existingUser.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
            existingUser.setRole(role);
            existingUser.setAccountEnabled(true);
            existingUser.setEmailVerified(true);
            return userRepository.save(existingUser);
        }).orElseGet(() -> {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setAuthProvider(AuthProvider.LOCAL);
            user.setEmailVerified(true);
            user.setAccountEnabled(true);
            return userRepository.save(user);
        });
    }

    private void seedPermission(String code, String name, String module) {
        if (permissionRepository.findByCode(code).isEmpty()) {
            Permission p = new Permission();
            p.setCode(code);
            p.setName(name);
            p.setModule(module);
            permissionRepository.save(p);
        }
    }

    private void seedGroup(String code, String name, String desc) {
        if (groupRepository.findByCode(code).isEmpty()) {
            AcademicGroup ag = new AcademicGroup();
            ag.setCode(code);
            ag.setName(name);
            ag.setDescription(desc);
            ag.setActive(true);
            groupRepository.save(ag);
        }
    }

    private void seedSection(String name, String branchGroup, String year, String academicYear, int capacity) {
        if (!sectionRepository.existsByNameIgnoreCaseAndBranchGroupAndIntermediateYearAndAcademicYear(name, branchGroup, year, academicYear)) {
            AcademicSection sec = new AcademicSection();
            sec.setName(name);
            sec.setBranchGroup(branchGroup);
            sec.setIntermediateYear(year);
            sec.setAcademicYear(academicYear);
            sec.setCapacity(capacity);
            sec.setActive(true);
            sectionRepository.save(sec);
        }
    }

    private DocumentType seedDocumentType(String code, String name, String category, String desc, boolean required, boolean expiry) {
        return documentTypeRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            DocumentType dt = new DocumentType();
            dt.setCode(code);
            dt.setName(name);
            dt.setCategory(DocumentCategory.valueOf(category));
            dt.setDescription(desc);
            dt.setRequiredByDefault(required);
            dt.setHasExpiry(expiry);
            dt.setActive(true);
            return documentTypeRepository.save(dt);
        });
    }

    private Faculty seedFaculty(String email, String fullName, String password, Role role, String facultyId, String empId, String fname, String lname, String gender, String mobile, String desig, String qual, String dept, String group) {
        User user = seedUser(email, fullName, password, role);
        Faculty faculty = facultyRepository.findByEmail(email)
                .orElseGet(() -> facultyRepository.findByFacultyId(facultyId).orElse(null));

        if (faculty != null) {
            faculty.setUser(user);
            faculty.setStatus("ACTIVE");
            return facultyRepository.save(faculty);
        }

        Faculty f = new Faculty();
        f.setFacultyId(facultyId);
        f.setUser(user);
        f.setEmployeeId(empId);
        f.setFirstName(fname);
        f.setLastName(lname);
        f.setFullName(fullName);
        f.setGender(gender);
        f.setMobileNumber(mobile);
        f.setEmail(email);
        f.setDesignation(desig);
        f.setQualification(qual);
        f.setDepartment(dept);
        f.setPrimaryGroup(group);
        f.setJoiningDate(LocalDate.of(2022, 6, 1));
        f.setEmploymentType("PERMANENT");
        f.setStatus("ACTIVE");
        return facultyRepository.save(f);
    }

    private void seedAssignment(Faculty faculty, String group, String year, String section, String academicYear, String subject) {
        boolean exists = assignmentRepository.findByFacultyId(faculty.getId()).stream()
                .anyMatch(a -> a.getSection().equalsIgnoreCase(section) && a.getBranchGroup().equalsIgnoreCase(group) && a.getIntermediateYear().equalsIgnoreCase(year));
        if (!exists) {
            FacultyAssignment fa = new FacultyAssignment();
            fa.setFaculty(faculty);
            fa.setBranchGroup(group);
            fa.setIntermediateYear(year);
            fa.setSection(section);
            fa.setAcademicYear(academicYear);
            fa.setSubjectName(subject);
            fa.setActive(true);
            assignmentRepository.save(fa);
        }
    }

    @SuppressWarnings("unused")
    private Student seedStudent(String studentId, String rollNo, String fname, String lname, String gender, String dob, String group, String year, String sec, String academicYear, String email, String mobile, String fatherName, String parentMobile) {
        return studentRepository.findByStudentId(studentId).orElseGet(() -> {
            Student s = new Student();
            s.setStudentId(studentId);
            s.setRollNumber(rollNo);
            s.setFirstName(fname);
            s.setLastName(lname);
            s.setFullName(fname + " " + lname);
            s.setGender(gender);
            s.setDateOfBirth(LocalDate.parse(dob));
            s.setStatus(StudentStatus.ACTIVE);

            StudentContactDetail cd = new StudentContactDetail();
            cd.setStudent(s);
            cd.setMobileNumber(mobile);
            cd.setEmail(email);
            cd.setAddress("H.No 12-4, College Road");
            cd.setCity("Hyderabad");
            cd.setDistrict("Hyderabad");
            cd.setState("Telangana");
            cd.setPinCode("500001");
            s.setContactDetail(cd);

            StudentParentDetail pd = new StudentParentDetail();
            pd.setStudent(s);
            pd.setFatherName(fatherName);
            pd.setMotherName("Lakshmi " + lname);
            pd.setParentMobile(parentMobile);
            s.setParentDetail(pd);

            StudentAcademicDetail ad = new StudentAcademicDetail();
            ad.setStudent(s);
            ad.setBranchGroup(group);
            ad.setIntermediateYear(year);
            ad.setSection(sec);
            ad.setBatch("2026-2028");
            ad.setAcademicYear(academicYear);
            ad.setAdmissionDate(LocalDate.of(2026, 6, 1));
            ad.setHostelDayScholar("DAY_SCHOLAR");
            s.setAcademicDetail(ad);

            return studentRepository.save(s);
        });
    }

    @SuppressWarnings("unused")
    private void seedStudentDocument(Student student, DocumentType docType, String docNum, String status, String path, String origName) {
        // Guard: skip if a document already exists for this student + document type
        if (studentDocumentRepository.findByStudentIdAndDocumentTypeId(student.getId(), docType.getId()).isPresent()) {
            return;
        }
        StudentDocument sd = new StudentDocument();
        sd.setStudent(student);
        sd.setDocumentType(docType);
        sd.setDocumentNumber(docNum);
        sd.setStatus(DocumentStatus.valueOf(status));
        sd.setStoragePath(path);
        sd.setOriginalFileName(origName);
        sd.setStoredFileName(origName);
        sd.setMimeType("application/pdf");
        sd.setFileSize(512000L);
        studentDocumentRepository.save(sd);
    }
}
