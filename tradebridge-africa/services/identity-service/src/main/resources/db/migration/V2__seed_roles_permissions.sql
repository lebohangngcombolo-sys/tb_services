-- TradeBridge Africa — Identity seed: roles and permissions
-- Aligned with PermissionCode from security-starter.
-- UUIDs are generated with gen_random_uuid() for portability.

-- ============================================================ ROLES
INSERT INTO roles (id, code, name, description, system_role) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',        'Super Administrator',   'Unrestricted platform access',                 TRUE),
    (gen_random_uuid(), 'ADMIN',              'Administrator',         'Administrative access except super-admin ops', TRUE),
    (gen_random_uuid(), 'KYC_ANALYST',        'KYC Analyst',           'Reviews KYC cases',                            TRUE),
    (gen_random_uuid(), 'KYC_SUPERVISOR',     'KYC Supervisor',        'Assigns and oversees KYC reviews',              TRUE),
    (gen_random_uuid(), 'COMPLIANCE_OFFICER', 'Compliance Officer',    'Compliance-related operations',                TRUE),
    (gen_random_uuid(), 'SUPPORT',            'Support Agent',         'Customer support operations',                  TRUE),
    (gen_random_uuid(), 'CUSTOMER',           'Customer',              'Standard customer account',                    TRUE),
    (gen_random_uuid(), 'SERVICE',            'Service',               'Service-to-service identity',                  TRUE);

-- ============================================================ PERMISSIONS
INSERT INTO permissions (id, code, resource, action, description) VALUES
    (gen_random_uuid(), 'identity.user.read',         'identity.user',  'read',   'View user records'),
    (gen_random_uuid(), 'identity.user.admin',        'identity.user',  'admin',  'Administer user accounts'),
    (gen_random_uuid(), 'identity.role.read',         'identity.role',  'read',   'View roles'),
    (gen_random_uuid(), 'identity.role.write',        'identity.role',  'write',  'Create and modify roles'),
    (gen_random_uuid(), 'identity.permission.read',   'identity.permission', 'read', 'View permissions'),

    (gen_random_uuid(), 'country.read',               'country',        'read',   'View country information'),
    (gen_random_uuid(), 'country.admin',              'country',        'admin',  'Administer country data'),
    (gen_random_uuid(), 'country.config.publish',     'country.config', 'publish','Publish country configuration'),
    (gen_random_uuid(), 'country.config.rollback',    'country.config', 'rollback','Roll back country configuration'),

    (gen_random_uuid(), 'customer.read',              'customer',       'read',   'View customer data'),
    (gen_random_uuid(), 'customer.admin',             'customer',       'admin',  'Administer customer data'),
    (gen_random_uuid(), 'organization.read',          'organization',   'read',   'View organization data'),
    (gen_random_uuid(), 'organization.manage',        'organization',   'manage', 'Modify organizations'),
    (gen_random_uuid(), 'organization.submit',        'organization',   'submit', 'Submit organization onboarding'),

    (gen_random_uuid(), 'kyc.case.read',              'kyc.case',       'read',   'View KYC cases'),
    (gen_random_uuid(), 'kyc.case.review',            'kyc.case',       'review', 'Review KYC cases'),
    (gen_random_uuid(), 'kyc.case.approve',           'kyc.case',       'approve','Approve KYC cases'),
    (gen_random_uuid(), 'kyc.case.reject',            'kyc.case',       'reject', 'Reject KYC cases'),
    (gen_random_uuid(), 'kyc.decision.view',          'kyc.decision',   'view',   'View KYC decisions'),

    (gen_random_uuid(), 'notification.submit',        'notification',   'submit', 'Submit notifications'),
    (gen_random_uuid(), 'notification.read',          'notification',   'read',   'Read notification records'),
    (gen_random_uuid(), 'notification.template.admin', 'notification.template', 'admin', 'Administer notification templates'),
    (gen_random_uuid(), 'notification.delivery.admin', 'notification.delivery', 'admin', 'Administer notification deliveries'),

    (gen_random_uuid(), 'service.identity',           'service.identity', 'call', 'Call identity internal APIs'),
    (gen_random_uuid(), 'service.notification',       'service.notification', 'call', 'Call notification internal APIs'),
    (gen_random_uuid(), 'service.country',            'service.country', 'call', 'Call country APIs'),
    (gen_random_uuid(), 'service.customer',           'service.customer','call', 'Call customer APIs');

-- ============================================================ ROLE → PERMISSIONS

-- SUPER_ADMIN: all permissions
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- ADMIN: most permissions except role.write and country.config.publish/rollback
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code NOT IN (
    'identity.role.write',
    'country.config.publish',
    'country.config.rollback'
  );

-- KYC_ANALYST: read + review KYC
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'KYC_ANALYST'
  AND p.code IN (
    'kyc.case.read',
    'kyc.case.review',
    'kyc.decision.view',
    'organization.read',
    'customer.read'
  );

-- KYC_SUPERVISOR: KYC_ANALYST + assign/approve
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'KYC_SUPERVISOR'
  AND p.code IN (
    'kyc.case.read',
    'kyc.case.review',
    'kyc.case.approve',
    'kyc.case.reject',
    'kyc.decision.view',
    'organization.read',
    'customer.read'
  );

-- COMPLIANCE_OFFICER: read-heavy across KYC + customer
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'COMPLIANCE_OFFICER'
  AND p.code IN (
    'customer.read',
    'organization.read',
    'kyc.case.read',
    'kyc.decision.view',
    'identity.user.read'
  );

-- SUPPORT: read-only basics
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPPORT'
  AND p.code IN (
    'identity.user.read',
    'customer.read',
    'organization.read',
    'notification.read'
  );

-- CUSTOMER: self-service permissions are handled via ownership, not role grants.
-- No row-level permissions assigned here — services enforce ownership.

-- SERVICE: call permissions
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SERVICE'
  AND p.code IN (
    'service.identity',
    'service.notification',
    'service.country',
    'service.customer'
  );
