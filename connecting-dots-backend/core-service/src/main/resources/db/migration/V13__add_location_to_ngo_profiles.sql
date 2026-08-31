-- ==============================================================================
-- V13: Add location to NGO profiles
-- ==============================================================================

ALTER TABLE ngo_profiles ADD COLUMN location VARCHAR(255) DEFAULT 'Global Community';
