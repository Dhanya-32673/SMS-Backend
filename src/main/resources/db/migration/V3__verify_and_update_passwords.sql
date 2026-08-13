-- ====================================================================
-- SQL SCRIPT: VERIFY & RESET ADMIN BCRYPT PASSWORD HASH
-- ====================================================================

-- 1. Inspection query to check current stored hashes
SELECT id, email, full_name, auth_provider, password_hash, updated_at
FROM users 
WHERE email IN ('dhanyaande@gmail.com', 'admin@college.edu');

-- 2. BCrypt update statement setting 'AdminPass123!' hash (a.UnVuG9HHgffUDAlk8qfOUVGkqRzgVym502L1Gy95vmsA1g.yRkS)
UPDATE users 
SET password_hash = 'a.UnVuG9HHgffUDAlk8qfOUVGkqRzgVym502L1Gy95vmsA1g.yRkS',
    updated_at = NOW()
WHERE email IN ('dhanyaande@gmail.com', 'admin@college.edu');
