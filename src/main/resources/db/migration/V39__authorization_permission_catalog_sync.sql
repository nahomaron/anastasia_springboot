INSERT INTO permissions (name, description)
VALUES
    ('VIEW_CALENDAR', 'Can view the calendar'),
    ('MANAGE_CALENDAR', 'Can create and manage calendar entries'),
    ('DELETE_GROUPS', 'Can delete groups')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name NOT IN ('MANAGE_TENANTS', 'VIEW_ALL_DATA', 'OWN_SUBSCRIPTION')
WHERE r.role_name = 'PRIMARY_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING')
WHERE r.role_name = 'OWNER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'VIEW_TENANT_USERS'
WHERE r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'VIEW_MEMBERS',
    'ADD_MEMBERS',
    'EDIT_MEMBERS',
    'DELETE_MEMBERS',
    'SMS_MEMBERS',
    'VIEW_CHILDREN',
    'VIEW_GROUPS',
    'VIEW_EVENTS',
    'MANAGE_REQUESTS',
    'VIEW_STAFF',
    'VIEW_CALENDAR',
    'VIEW_PRIESTS',
    'VIEW_PRIEST_ASSIGNMENTS',
    'APPROVE_MEMBERSHIP_AS_PRIEST',
    'VIEW_PRIEST_DASHBOARD',
    'ADVANCED_SEARCH_MEMBERS'
)
WHERE r.role_name = 'PRIEST'
ON CONFLICT DO NOTHING;
