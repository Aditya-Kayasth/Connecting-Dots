-- ==============================================================================
-- V11: Drop and Replace users_role_check Constraint
-- Resolves constraint violation when inserting default ADMIN user
-- ==============================================================================

-- 1. Drop the old stale check constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- 2. Create the updated check constraint containing all three roles
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('NGO', 'CONTRIBUTOR', 'ADMIN'));
