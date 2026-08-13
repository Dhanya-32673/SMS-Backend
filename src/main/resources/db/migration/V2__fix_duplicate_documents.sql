-- =============================================================================
-- Migration: Fix Duplicate Student Documents & Add Unique Constraint
-- Database: PostgreSQL 17 / Supabase
-- =============================================================================

-- 1. Inspect duplicate records (Diagnostic query)
-- SELECT student_id, document_type_id, COUNT(*) 
-- FROM student_documents 
-- GROUP BY student_id, document_type_id 
-- HAVING COUNT(*) > 1;

-- 2. Clean up duplicate records keeping only the latest document record per student & document type
DELETE FROM student_documents
WHERE id NOT IN (
    SELECT MAX(id)
    FROM student_documents
    GROUP BY student_id, document_type_id
);

-- 3. Safely apply Unique Constraint to prevent future duplicate uploads
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_student_document'
    ) THEN
        ALTER TABLE student_documents
        ADD CONSTRAINT uq_student_document UNIQUE (student_id, document_type_id);
    END IF;
END $$;
