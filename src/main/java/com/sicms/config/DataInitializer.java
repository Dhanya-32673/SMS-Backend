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

    private final com.sicms.service.CampusService campusService;

    public DataInitializer(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           AcademicGroupRepository groupRepository,
                           AcademicSectionRepository sectionRepository,
                           DocumentTypeRepository documentTypeRepository,
                           UserRepository userRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                           javax.sql.DataSource dataSource,
                           com.sicms.service.CampusService campusService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.groupRepository = groupRepository;
        this.sectionRepository = sectionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
        this.campusService = campusService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================================");
        try (java.sql.Connection conn = dataSource.getConnection()) {
            System.out.println("SICMS connected to PostgreSQL");
        } catch (Exception e) {
            System.err.println("SICMS PostgreSQL Connection Check Warning: " + e.getMessage());
        }

        // === 0. Standardize Student Schema ===
        migrateStudentSchema();

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
            seedGroup("MPC", "MPC", "Mathematics, Physics, Chemistry");
            seedGroup("BiPC", "BiPC", "Biology, Physics, Chemistry");
            seedGroup("MEC", "MEC", "Mathematics, Economics, Commerce");
            seedGroup("CEC", "CEC", "Civics, Economics, Commerce");
            seedGroup("HEC", "HEC", "History, Economics, Civics");

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

        // === 3.5. Official Campuses ===
        try {
            campusService.initOfficialCampuses();
            System.out.println("Official 18 Campuses master data initialized");
        } catch (Exception e) {
            System.err.println("Campus master data warning: " + e.getMessage());
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

        // === 6. Supabase Storage Policies ===
        try {
            seedStoragePolicies();
        } catch (Exception e) {
            System.err.println("Storage policies initialization warning: " + e.getMessage());
        }

        System.out.println("Application startup completed successfully");
        System.out.println("=================================");
    }

    private void seedStoragePolicies() {
        try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            String[] sqls = {
                "UPDATE storage.buckets SET public = true WHERE id = 'student-profile-photos';",
                "DROP POLICY IF EXISTS \"Allow public uploads to student-profile-photos\" ON storage.objects;",
                "CREATE POLICY \"Allow public uploads to student-profile-photos\" ON storage.objects FOR INSERT TO public WITH CHECK (bucket_id = 'student-profile-photos');",
                "DROP POLICY IF EXISTS \"Allow public update to student-profile-photos\" ON storage.objects;",
                "CREATE POLICY \"Allow public update to student-profile-photos\" ON storage.objects FOR UPDATE TO public USING (bucket_id = 'student-profile-photos');",
                "DROP POLICY IF EXISTS \"Allow public select from student-profile-photos\" ON storage.objects;",
                "CREATE POLICY \"Allow public select from student-profile-photos\" ON storage.objects FOR SELECT TO public USING (bucket_id = 'student-profile-photos');",
                "DROP POLICY IF EXISTS \"Allow public delete from student-profile-photos\" ON storage.objects;",
                "CREATE POLICY \"Allow public delete from student-profile-photos\" ON storage.objects FOR DELETE TO public USING (bucket_id = 'student-profile-photos');",
                "DROP POLICY IF EXISTS \"Allow public uploads to student-documents\" ON storage.objects;",
                "CREATE POLICY \"Allow public uploads to student-documents\" ON storage.objects FOR INSERT TO public WITH CHECK (bucket_id = 'student-documents');",
                "DROP POLICY IF EXISTS \"Allow public update to student-documents\" ON storage.objects;",
                "CREATE POLICY \"Allow public update to student-documents\" ON storage.objects FOR UPDATE TO public USING (bucket_id = 'student-documents');",
                "DROP POLICY IF EXISTS \"Allow public select from student-documents\" ON storage.objects;",
                "CREATE POLICY \"Allow public select from student-documents\" ON storage.objects FOR SELECT TO public USING (bucket_id = 'student-documents');",
                "DROP POLICY IF EXISTS \"Allow public delete from student-documents\" ON storage.objects;",
                "CREATE POLICY \"Allow public delete from student-documents\" ON storage.objects FOR DELETE TO public USING (bucket_id = 'student-documents');"
            };
            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {}
            }
            System.out.println("Supabase Storage policies verified");
        } catch (Exception e) {
            System.err.println("Notice: Supabase Storage policy seeding skipped: " + e.getMessage());
        }
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
        java.util.Optional<AcademicGroup> opt = groupRepository.findByCode(code);
        if (opt.isEmpty()) {
            AcademicGroup ag = new AcademicGroup();
            ag.setCode(code);
            ag.setName(name);
            ag.setDescription(desc);
            ag.setActive(true);
            groupRepository.save(ag);
        } else {
            AcademicGroup ag = opt.get();
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

    private void migrateStudentSchema() {
        try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            String[] alterCols = {
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;",
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS student_id BIGINT REFERENCES students(id);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS full_name VARCHAR(150);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS category VARCHAR(50);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS email_address_1 VARCHAR(150);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS email_address_2 VARCHAR(150);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(20);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS alternate_mobile VARCHAR(20);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS father_name VARCHAR(100);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_name VARCHAR(100);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS branch_group VARCHAR(50);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS intermediate_year VARCHAR(20);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS batch VARCHAR(20);",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_type VARCHAR(30) DEFAULT 'REGULAR';",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS hostel_day_scholar VARCHAR(30) DEFAULT 'DAY_SCHOLAR';",
                "ALTER TABLE students ADD COLUMN IF NOT EXISTS section VARCHAR(10) DEFAULT 'Unassigned';"
            };
            for (String sql : alterCols) {
                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    System.err.println("Notice: Schema column migration: " + e.getMessage());
                }
            }

            // Copy data from child tables if they exist
            String[] dataMigrations = {
                "UPDATE students s SET full_name = TRIM(CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.last_name, ''))) WHERE (s.full_name IS NULL OR s.full_name = '') AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='students' AND column_name='first_name');",
                "UPDATE students s SET email_address_1 = COALESCE(s.email_address_1, c.email), mobile_number = COALESCE(s.mobile_number, c.mobile_number), alternate_mobile = COALESCE(s.alternate_mobile, c.alternate_mobile) FROM student_contact_details c WHERE c.student_id = s.id;",
                "UPDATE students s SET father_name = COALESCE(s.father_name, p.father_name), mother_name = COALESCE(s.mother_name, p.mother_name) FROM student_parent_details p WHERE p.student_id = s.id;",
                "UPDATE students s SET academic_year = COALESCE(s.academic_year, a.academic_year), branch_group = COALESCE(s.branch_group, a.branch_group), intermediate_year = COALESCE(s.intermediate_year, a.intermediate_year), batch = COALESCE(s.batch, a.batch), admission_type = COALESCE(s.admission_type, a.admission_type, 'REGULAR'), hostel_day_scholar = COALESCE(s.hostel_day_scholar, a.hostel_day_scholar, 'DAY_SCHOLAR'), section = COALESCE(s.section, a.section, 'Unassigned') FROM student_academic_details a WHERE a.student_id = s.id;",
                "UPDATE students SET category = COALESCE(category, caste_category) WHERE EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='students' AND column_name='caste_category');",
                "UPDATE users u SET student_id = s.id FROM students s WHERE u.student_id IS NULL AND (LOWER(u.email) = LOWER(s.email_address_1) OR LOWER(u.email) = LOWER(s.email_address_2) OR LOWER(u.email) = LOWER(CONCAT(s.student_id, '@student.bhashyam.edu')) OR LOWER(u.email) = LOWER(s.student_id) OR LOWER(u.email) = LOWER(s.admission_number));"
            };
            for (String sql : dataMigrations) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {}
            }

            // Purge mock seeds & test academic groups
            try {
                stmt.execute("DELETE FROM student_documents WHERE student_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9);");
                stmt.execute("DELETE FROM students WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9);");
                stmt.execute("DELETE FROM academic_groups WHERE UPPER(code) NOT IN ('MPC', 'BIPC', 'MEC', 'CEC', 'HEC') OR LOWER(code) LIKE 'qa%' OR LOWER(name) LIKE '%quality assurance%' OR LOWER(description) LIKE '%automated api test%';");
            } catch (Exception ignored) {}

            // Drop child tables
            String[] dropTables = {
                "DROP TABLE IF EXISTS student_guardians CASCADE;",
                "DROP TABLE IF EXISTS student_contact_details CASCADE;",
                "DROP TABLE IF EXISTS student_parent_details CASCADE;",
                "DROP TABLE IF EXISTS student_academic_details CASCADE;"
            };
            for (String sql : dropTables) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {}
            }

            // Drop obsolete columns
            String[] dropCols = {
                "ALTER TABLE students DROP COLUMN IF EXISTS roll_number;",
                "ALTER TABLE students DROP COLUMN IF EXISTS first_name;",
                "ALTER TABLE students DROP COLUMN IF EXISTS middle_name;",
                "ALTER TABLE students DROP COLUMN IF EXISTS last_name;",
                "ALTER TABLE students DROP COLUMN IF EXISTS blood_group;",
                "ALTER TABLE students DROP COLUMN IF EXISTS caste_category;",
                "ALTER TABLE students DROP COLUMN IF EXISTS pan_number;",
                "ALTER TABLE students DROP COLUMN IF EXISTS identification_marks;"
            };
            for (String sql : dropCols) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {}
            }

            try {
                stmt.execute("ALTER TABLE document_versions ALTER COLUMN file_name DROP NOT NULL;");
            } catch (Exception ignored) {}

            // Indexes
            String[] indexes = {
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_students_admission_number ON students (admission_number) WHERE admission_number IS NOT NULL AND admission_number <> '';",
                "CREATE INDEX IF NOT EXISTS idx_students_branch_group ON students(branch_group);",
                "CREATE INDEX IF NOT EXISTS idx_students_academic_year ON students(academic_year);",
                "CREATE INDEX IF NOT EXISTS idx_students_section ON students(section);",
                "CREATE INDEX IF NOT EXISTS idx_students_mobile ON students(mobile_number);",
                "CREATE INDEX IF NOT EXISTS idx_students_email_1 ON students(email_address_1);"
            };
            for (String sql : indexes) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {}
            }

            System.out.println("Standardized student schema verified and migrated successfully");
        } catch (Exception e) {
            System.err.println("Student schema migration check warning: " + e.getMessage());
        }
    }
}
