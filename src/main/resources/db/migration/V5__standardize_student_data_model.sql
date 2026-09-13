-- ============================================================
-- V5__standardize_student_data_model.sql
-- SICMS Student Data Model Standardization to 22 Canonical Columns
-- ============================================================

-- 1. ADD NEW APPROVED STUDENT COLUMNS TO STUDENTS TABLE
ALTER TABLE students ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE students ADD COLUMN IF NOT EXISTS email_address_1 VARCHAR(150);
ALTER TABLE students ADD COLUMN IF NOT EXISTS email_address_2 VARCHAR(150);
ALTER TABLE students ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS alternate_mobile VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS father_name VARCHAR(100);
ALTER TABLE students ADD COLUMN IF NOT EXISTS mother_name VARCHAR(100);
ALTER TABLE students ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS branch_group VARCHAR(50);
ALTER TABLE students ADD COLUMN IF NOT EXISTS intermediate_year VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS batch VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_type VARCHAR(30) DEFAULT 'REGULAR';
ALTER TABLE students ADD COLUMN IF NOT EXISTS hostel_day_scholar VARCHAR(30) DEFAULT 'DAY_SCHOLAR';
ALTER TABLE students ADD COLUMN IF NOT EXISTS section VARCHAR(10) DEFAULT 'Unassigned';

-- 2. MIGRATE DATA FROM OBSOLETE CHILD TABLES BEFORE DROPPING
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'student_contact_details') THEN
        UPDATE students s
        SET 
            email_address_1 = COALESCE(s.email_address_1, c.email),
            mobile_number = COALESCE(s.mobile_number, c.mobile_number),
            alternate_mobile = COALESCE(s.alternate_mobile, c.alternate_mobile)
        FROM student_contact_details c
        WHERE c.student_id = s.id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'student_parent_details') THEN
        UPDATE students s
        SET 
            father_name = COALESCE(s.father_name, p.father_name),
            mother_name = COALESCE(s.mother_name, p.mother_name)
        FROM student_parent_details p
        WHERE p.student_id = s.id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'student_academic_details') THEN
        UPDATE students s
        SET 
            academic_year = COALESCE(s.academic_year, a.academic_year),
            branch_group = COALESCE(s.branch_group, a.branch_group),
            intermediate_year = COALESCE(s.intermediate_year, a.intermediate_year),
            batch = COALESCE(s.batch, a.batch),
            admission_type = COALESCE(s.admission_type, a.admission_type, 'REGULAR'),
            hostel_day_scholar = COALESCE(s.hostel_day_scholar, a.hostel_day_scholar, 'DAY_SCHOLAR'),
            section = COALESCE(s.section, a.section, 'Unassigned')
        FROM student_academic_details a
        WHERE a.student_id = s.id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'students' AND column_name = 'caste_category') THEN
        UPDATE students SET category = COALESCE(category, caste_category);
    END IF;
END $$;

-- 3. PURGE OBSOLETE MOCK SEED STUDENTS (IDs 1 to 9)
DELETE FROM student_documents WHERE student_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9);
DELETE FROM students WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9);

-- 4. DROP OBSOLETE CHILD TABLES
DROP TABLE IF EXISTS student_guardians CASCADE;
DROP TABLE IF EXISTS student_contact_details CASCADE;
DROP TABLE IF EXISTS student_parent_details CASCADE;
DROP TABLE IF EXISTS student_academic_details CASCADE;

-- 5. DROP OBSOLETE STUDENT COLUMNS
ALTER TABLE students
    DROP COLUMN IF EXISTS roll_number,
    DROP COLUMN IF EXISTS first_name,
    DROP COLUMN IF EXISTS middle_name,
    DROP COLUMN IF EXISTS last_name,
    DROP COLUMN IF EXISTS blood_group,
    DROP COLUMN IF EXISTS caste_category,
    DROP COLUMN IF EXISTS pan_number,
    DROP COLUMN IF EXISTS identification_marks;

-- 6. PERFORMANCE & INTEGRITY INDEXES
CREATE UNIQUE INDEX IF NOT EXISTS uk_students_admission_number ON students (admission_number) WHERE admission_number IS NOT NULL AND admission_number <> '';
CREATE INDEX IF NOT EXISTS idx_students_branch_group ON students(branch_group);
CREATE INDEX IF NOT EXISTS idx_students_academic_year ON students(academic_year);
CREATE INDEX IF NOT EXISTS idx_students_section ON students(section);
CREATE INDEX IF NOT EXISTS idx_students_mobile ON students(mobile_number);
CREATE INDEX IF NOT EXISTS idx_students_email_1 ON students(email_address_1);
