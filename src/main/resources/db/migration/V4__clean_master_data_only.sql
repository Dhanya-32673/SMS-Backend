-- ====================================================================
-- MIGRATION V4: CLEAN MASTER DATA & SYSTEM SETUP SCHEMA
-- ====================================================================

-- Ensure index on roles for quick lookup during initial admin setup check
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(role_name);

-- Ensure index on users email for case-insensitive authentication queries
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(LOWER(email));
