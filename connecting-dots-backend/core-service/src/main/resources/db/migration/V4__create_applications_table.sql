CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL REFERENCES problem_statements(id) ON DELETE CASCADE,
    contributor_profile_id UUID NOT NULL REFERENCES contributor_profiles(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_contributor_problem UNIQUE (problem_id, contributor_profile_id)
);

CREATE INDEX idx_applications_problem_id ON applications(problem_id);
CREATE INDEX idx_applications_contributor_id ON applications(contributor_profile_id);