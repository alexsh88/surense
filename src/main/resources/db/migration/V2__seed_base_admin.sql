-- Seed the bootstrap ADMIN account.
-- Password: Admin@1234  (BCrypt cost 12)
-- This account is always present in every environment.
-- Change the password hash via the API after first deploy.
INSERT INTO users (id, username, password_hash, full_name, role, agent_id, active, version, created_at, updated_at, created_by, updated_by)
VALUES (
    UUID_TO_BIN('00000000-0000-7001-8000-000000000001'),
    'admin',
    '$2a$12$JQoPsFORA7qY0GFeD8LzIuglPD/uE/AHXUSTsqc317RvPcPuMrPzK',
    'System Admin',
    'ADMIN',
    NULL,
    TRUE,
    0,
    NOW(3),
    NOW(3),
    NULL,
    NULL
);
