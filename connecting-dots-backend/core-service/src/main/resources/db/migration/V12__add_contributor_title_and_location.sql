-- ==============================================================================
-- V12: Add title and location to contributor profiles
-- ==============================================================================

ALTER TABLE contributor_profiles ADD COLUMN title VARCHAR(255) DEFAULT 'Technical Contributor';
ALTER TABLE contributor_profiles ADD COLUMN location VARCHAR(255) DEFAULT 'Community Member';
