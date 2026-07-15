INSERT INTO permissions (name, description)
VALUES (
    'VIEW_APPOINTMENTS',
    'Can view appointment schedules and details'
)
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'VIEW_APPOINTMENTS'
WHERE r.role_name IN ('PLATFORM_ADMIN', 'PRIMARY_ADMIN', 'PRIEST')
ON CONFLICT DO NOTHING;
