ALTER TABLE ngo_profiles ADD COLUMN preferred_language VARCHAR(10) DEFAULT 'en';
ALTER TABLE contributor_profiles ADD COLUMN preferred_language VARCHAR(10) DEFAULT 'en';
