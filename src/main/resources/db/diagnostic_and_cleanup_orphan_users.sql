-- ============================================================================
-- DIAGNOSTIC & SAFE CLEANUP SCRIPT: EXISTING ORPHAN STUDENT USER ACCOUNTS
-- ============================================================================
-- NOTE: Prior to the bug fix in StudentService.java, deleting a student unlinked
-- the user account (u.setStudent(null); u.setAccountEnabled(false);) instead of
-- deleting the user account from the `users` table.
--
-- This script provides:
-- 1. A read-only diagnostic query to inspect orphan accounts safely.
-- 2. An explicit, safety-gated cleanup transaction that deletes ONLY verified,
--    disabled student orphan accounts, strictly excluding ADMIN and FACULTY.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PART 1: DIAGNOSTIC QUERY (READ ONLY - SAFE TO RUN AT ANY TIME)
-- ----------------------------------------------------------------------------
-- Inspect users that have the student role, no linked student record, and are disabled.
SELECT 
    u.id, 
    u.email, 
    u.full_name, 
    r.role_name, 
    u.account_enabled, 
    u.student_id,
    u.created_at
FROM users u
JOIN roles r ON u.role_id = r.id
WHERE r.role_name IN ('ROLE_STUDENT', 'STUDENT')
  AND u.student_id IS NULL
  AND u.account_enabled = false
  AND u.email NOT LIKE '%admin%'
ORDER BY u.id ASC;


-- ----------------------------------------------------------------------------
-- PART 2: DELIBERATE CLEANUP (TRANSACTIONAL & SAFETY-GATED)
-- ----------------------------------------------------------------------------
-- DO NOT RUN AUTOMATICALLY. Execute only after reviewing the output of Part 1.
-- This block safely deletes dependent records (tokens, OTPs) and then removes
-- the verified orphan student accounts.

/*
BEGIN;

-- Step 2a: Create temporary table of target orphan user IDs for safety
CREATE TEMP TABLE orphan_student_user_ids AS
SELECT u.id, u.email
FROM users u
JOIN roles r ON u.role_id = r.id
WHERE r.role_name IN ('ROLE_STUDENT', 'STUDENT')
  AND u.student_id IS NULL
  AND u.account_enabled = false
  AND u.email NOT LIKE '%admin%';

-- Step 2b: Clear foreign key references in dependent tables
DELETE FROM refresh_tokens 
WHERE user_id IN (SELECT id FROM orphan_student_user_ids);

DELETE FROM password_reset_otps 
WHERE user_id IN (SELECT id FROM orphan_student_user_ids);

DELETE FROM otp_verifications 
WHERE user_id IN (SELECT id FROM orphan_student_user_ids);

UPDATE export_audit_logs 
SET user_id = NULL 
WHERE user_id IN (SELECT id FROM orphan_student_user_ids);

UPDATE students 
SET created_by = NULL 
WHERE created_by IN (SELECT id FROM orphan_student_user_ids);

-- Step 2c: Delete verified orphan user records
DELETE FROM users 
WHERE id IN (SELECT id FROM orphan_student_user_ids);

-- Step 2d: Drop temporary table
DROP TABLE orphan_student_user_ids;

COMMIT;
*/
