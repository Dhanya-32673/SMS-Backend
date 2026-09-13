-- ============================================================
-- COLLEGE STUDENT INFORMATION & CERTIFICATE MANAGEMENT SYSTEM (SICMS)
-- COMPLETE RESET & REBUILD SCHEMA FOR SUPABASE POSTGRESQL
-- ============================================================

-- STEP 1: DROP AND RECREATE PUBLIC SCHEMA (CLEANS ALL OLD TABLES & CONSTRAINTS)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- STEP 2: CREATE SEQUENCES
CREATE SEQUENCE IF NOT EXISTS student_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS faculty_id_seq START WITH 100 INCREMENT BY 1;

-- ============================================================
-- PART 1: AUTHENTICATION & AUTHORIZATION SCHEMA
-- ============================================================

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    google_subject VARCHAR(255),
    profile_photo_url TEXT,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    student_id BIGINT REFERENCES students(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE
);

CREATE TABLE otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- ============================================================
-- PART 2: ACADEMIC STRUCTURES & DOCUMENT TYPES
-- ============================================================

CREATE TABLE academic_groups (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE academic_sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL,
    branch_group VARCHAR(50) NOT NULL,
    intermediate_year VARCHAR(20) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    capacity INT NOT NULL DEFAULT 60,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE document_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    required_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    has_expiry BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE document_requirements (
    id BIGSERIAL PRIMARY KEY,
    document_type_id BIGINT NOT NULL REFERENCES document_types(id) ON DELETE CASCADE,
    intermediate_year VARCHAR(20) NOT NULL,
    branch_group VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================
-- PART 3: FACULTY & STUDENTS SCHEMAS
-- ============================================================

CREATE TABLE faculty (
    id BIGSERIAL PRIMARY KEY,
    faculty_id VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    employee_id VARCHAR(50) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    last_name VARCHAR(50) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    date_of_birth DATE,
    mobile_number VARCHAR(20),
    alternate_mobile VARCHAR(20),
    email VARCHAR(150),
    qualification VARCHAR(100),
    experience VARCHAR(50),
    designation VARCHAR(100),
    department VARCHAR(100),
    employment_type VARCHAR(50),
    joining_date DATE,
    primary_group VARCHAR(50),
    address TEXT,
    city VARCHAR(50),
    district VARCHAR(50),
    state VARCHAR(50),
    pin_code VARCHAR(20),
    photo_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE faculty_assignments (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    branch_group VARCHAR(50) NOT NULL,
    intermediate_year VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    subject_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    student_id VARCHAR(30) NOT NULL UNIQUE,
    admission_number VARCHAR(50),
    full_name VARCHAR(150) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    nationality VARCHAR(50) DEFAULT 'Indian',
    religion VARCHAR(50),
    category VARCHAR(50),
    aadhaar_number VARCHAR(20),
    profile_photo_url TEXT,
    mobile_number VARCHAR(20) NOT NULL,
    alternate_mobile VARCHAR(20),
    email_address_1 VARCHAR(150) NOT NULL,
    email_address_2 VARCHAR(150),
    father_name VARCHAR(100) NOT NULL,
    mother_name VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    branch_group VARCHAR(50) NOT NULL,
    intermediate_year VARCHAR(20) NOT NULL,
    batch VARCHAR(20) NOT NULL,
    admission_type VARCHAR(30) DEFAULT 'REGULAR',
    hostel_day_scholar VARCHAR(30) NOT NULL DEFAULT 'DAY_SCHOLAR',
    section VARCHAR(10) NOT NULL DEFAULT 'Unassigned',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE student_documents (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    document_type_id BIGINT NOT NULL REFERENCES document_types(id) ON DELETE RESTRICT,
    document_number VARCHAR(100),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    storage_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    issued_by VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    rejected_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    notes TEXT,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    student_document_id BIGINT NOT NULL REFERENCES student_documents(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    notes TEXT,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================
-- PART 4: SEED DATA INSERTIONS FOR SUPABASE POSTGRESQL
-- ============================================================

-- 1. SEED ROLES
INSERT INTO roles (id, role_name, description)
VALUES 
    (1, 'ROLE_ADMIN', 'System Administrator'),
    (2, 'ROLE_FACULTY', 'Faculty Member'),
    (3, 'ROLE_STUDENT', 'Student Account')
ON CONFLICT (id) DO NOTHING;

-- 2. REFERENCE MASTER DATA ONLY (No hardcoded user seed data)

-- 3. SEED ACADEMIC GROUPS
INSERT INTO academic_groups (code, name, description, active)
VALUES 
    ('MPC', 'Maths, Physics, Chemistry', 'Intermediate Science Stream with Mathematics', TRUE),
    ('BiPC', 'Biology, Physics, Chemistry', 'Intermediate Medical Stream', TRUE),
    ('MEC', 'Maths, Economics, Commerce', 'Intermediate Commerce Stream with Mathematics', TRUE),
    ('CEC', 'Civics, Economics, Commerce', 'Intermediate Commerce Stream', TRUE),
    ('HEC', 'History, Economics, Civics', 'Intermediate Arts Stream', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 4. SEED ACADEMIC SECTIONS
INSERT INTO academic_sections (name, branch_group, intermediate_year, academic_year, capacity, active)
VALUES 
    ('A', 'MPC', '1st Year', '2026-2027', 60, TRUE),
    ('B', 'MPC', '1st Year', '2026-2027', 60, TRUE),
    ('A', 'MPC', '2nd Year', '2026-2027', 60, TRUE),
    ('A', 'BiPC', '1st Year', '2026-2027', 60, TRUE),
    ('B', 'MEC', '1st Year', '2026-2027', 60, TRUE),
    ('A', 'CEC', '2nd Year', '2026-2027', 60, TRUE),
    ('A', 'HEC', '1st Year', '2026-2027', 60, TRUE);

INSERT INTO faculty_assignments (faculty_id, branch_group, intermediate_year, section, academic_year, subject_name, active)
VALUES 
    (1, 'HEC', '1st Year', 'A', '2026-2027', 'History & Social Studies', TRUE),
    (1, 'MPC', '1st Year', 'A', '2026-2027', 'Physics & Mathematics', TRUE);

-- 5. SEED DOCUMENT TYPES
INSERT INTO document_types (code, name, category, description, required_by_default, active)
VALUES 
    ('SSC_MEMO', 'SSC / 10th Marks Memo', 'ACADEMIC', '10th class secondary school certificate memo', TRUE, TRUE),
    ('INTER_1ST_MEMO', 'Intermediate 1st Year Memo', 'ACADEMIC', 'Junior Intermediate marks memo', TRUE, TRUE),
    ('TRANSFER_CERT', 'Transfer Certificate (TC)', 'ADMISSION', 'Official institution transfer certificate', TRUE, TRUE),
    ('AADHAAR_DOC', 'Aadhaar Card Document', 'IDENTITY', 'Government issued Aadhaar identity card scan', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- PERFORMANCE INDEXES FOR SUB-1-SECOND QUERY EXECUTION
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_students_status ON students(status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_students_admission_number ON students (admission_number) WHERE admission_number IS NOT NULL AND admission_number <> '';
CREATE INDEX IF NOT EXISTS idx_students_branch_group ON students(branch_group);
CREATE INDEX IF NOT EXISTS idx_students_academic_year ON students(academic_year);
CREATE INDEX IF NOT EXISTS idx_students_section ON students(section);
CREATE INDEX IF NOT EXISTS idx_students_mobile ON students(mobile_number);
CREATE INDEX IF NOT EXISTS idx_students_email_1 ON students(email_address_1);
CREATE INDEX IF NOT EXISTS idx_student_docs_status ON student_documents(status);
CREATE INDEX IF NOT EXISTS idx_student_docs_student_status ON student_documents(student_id, status);
CREATE INDEX IF NOT EXISTS idx_faculty_assignments_faculty_id ON faculty_assignments(faculty_id);


