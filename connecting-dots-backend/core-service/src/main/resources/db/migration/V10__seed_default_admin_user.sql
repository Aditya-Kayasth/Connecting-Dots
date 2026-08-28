INSERT INTO users (id, email, password_hash, role, is_active, created_at, updated_at)
VALUES (
    '64c7a84e-5ad8-4b0d-bd90-12ac6e3ac932',
    'admin@connectingdots.org',
    '$2a$10$p91aUo60HR.usUPwHu878.Cg5Gz2l6bFfdCqVxCsg8Mn3jTLJ61Ai',
    'ADMIN',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
