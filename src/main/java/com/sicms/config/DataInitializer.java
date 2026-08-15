package com.sicms.config;

import com.sicms.entity.*;
import com.sicms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AcademicGroupRepository groupRepository;
    private final AcademicSectionRepository sectionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${app.admin.email:bhashyamgnt.edu@gmail.com}")
    private String adminEmail;

    public DataInitializer(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           AcademicGroupRepository groupRepository,
                           AcademicSectionRepository sectionRepository,
                           DocumentTypeRepository documentTypeRepository,
                           UserRepository userRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           javax.sql.DataSource dataSource) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================================");
        try (java.sql.Connection conn = dataSource.getConnection()) {
            System.out.println("SICMS connected to PostgreSQL");
        } catch (Exception e) {
            System.err.println("SICMS PostgreSQL Connection Check Warning: " + e.getMessage());
        }

        // === 1. Roles ===
        try {
            seedRole("ROLE_ADMIN", "System Administrator");
            seedRole("ROLE_FACULTY", "Faculty Member");
            seedRole("STUDENT", "Student User Role");
            System.out.println("Roles initialized");
        } catch (Exception e) {
            System.err.println("Roles initialization warning: " + e.getMessage());
        }

        // === 2. Permissions ===
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
            System.out.println("Permissions initialized");
        } catch (Exception e) {
            System.err.println("Permissions initialization warning: " + e.getMessage());
        }

        // === 3. Academic Groups & Sections ===
        try {
            seedGroup("MPC", "Maths, Physics, Chemistry", "Intermediate Science Stream");
            seedGroup("BiPC", "Biology, Physics, Chemistry", "Intermediate Biology Stream");
            seedGroup("MEC", "Maths, Economics, Commerce", "Intermediate Commerce/Maths Stream");
            seedGroup("CEC", "Civics, Economics, Commerce", "Intermediate Commerce Stream");
            seedGroup("HEC", "History, Economics, Civics", "Intermediate Arts Stream");

            seedSection("A", "MPC", "1st Year", "2026-2027", 60);
            seedSection("B", "MPC", "1st Year", "2026-2027", 60);
            seedSection("A", "MPC", "2nd Year", "2026-2027", 60);
            seedSection("A", "BiPC", "1st Year", "2026-2027", 60);
            seedSection("B", "MEC", "1st Year", "2026-2027", 60);
            seedSection("A", "CEC", "2nd Year", "2026-2027", 60);
            seedSection("A", "HEC", "1st Year", "2026-2027", 60);
            System.out.println("Academic master data initialized");
        } catch (Exception e) {
            System.err.println("Academic master data warning: " + e.getMessage());
        }

        // === 4. Document Types ===
        try {
            seedDocumentType("SSC_MEMO", "SSC / 10th Marks Memo", "ACADEMIC", "10th class secondary school certificate memo", true, false);
            seedDocumentType("INTER_1ST_MEMO", "Intermediate 1st Year Memo", "ACADEMIC", "Junior Intermediate marks memo", true, false);
            seedDocumentType("TRANSFER_CERT", "Transfer Certificate (TC)", "ADMISSION", "Official institution transfer certificate", true, false);
            seedDocumentType("AADHAAR_DOC", "Aadhaar Card Document", "IDENTITY", "Government issued Aadhaar identity card scan", true, false);
            System.out.println("Document types initialized");
        } catch (Exception e) {
            System.err.println("Document types warning: " + e.getMessage());
        }

        // === 5. Admin User ===
        try {
            seedAdminUser(adminEmail);
        } catch (Exception e) {
            System.err.println("Admin user initialization warning: " + e.getMessage());
        }

        System.out.println("Application startup completed successfully");
        System.out.println("=================================");
    }

    private void seedAdminUser(String email) {
        if (email == null || email.isBlank()) return;
        String cleanEmail = email.trim().toLowerCase();
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "System Administrator")));

        java.util.Optional<User> existingUser = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (existingUser.isEmpty()) {
            User admin = new User();
            admin.setFullName("Bhashyam Administrator");
            admin.setEmail(cleanEmail);
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(adminRole);
            admin.setAuthProvider(AuthProvider.LOCAL);
            admin.setEmailVerified(true);
            admin.setAccountEnabled(true);
            userRepository.save(admin);
            System.out.println("Default Admin user created: " + cleanEmail);
        } else {
            User user = existingUser.get();
            boolean updated = false;
            if (user.getRole() == null || !"ROLE_ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
                user.setRole(adminRole);
                updated = true;
            }
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                updated = true;
            }
            if (!Boolean.TRUE.equals(user.getAccountEnabled())) {
                user.setAccountEnabled(true);
                updated = true;
            }
            if (updated) {
                userRepository.save(user);
                System.out.println("Admin user updated with ROLE_ADMIN: " + cleanEmail);
            }
        }
    }

    private void seedRole(String name, String desc) {
        if (roleRepository.findByRoleName(name).isEmpty()) {
            roleRepository.save(new Role(name, desc));
        }
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

    private void seedDocumentType(String code, String name, String category, String desc, boolean required, boolean expiry) {
        if (documentTypeRepository.findByCodeIgnoreCase(code).isEmpty()) {
            DocumentType dt = new DocumentType();
            dt.setCode(code);
            dt.setName(name);
            dt.setCategory(DocumentCategory.valueOf(category));
            dt.setDescription(desc);
            dt.setRequiredByDefault(required);
            dt.setHasExpiry(expiry);
            dt.setActive(true);
            documentTypeRepository.save(dt);
        }
    }
}
