CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(120) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    email VARCHAR(254),
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE UNIQUE INDEX uq_users_username_lower ON users (LOWER(username));

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_roles_pair UNIQUE (user_id, role_id)
);
CREATE INDEX ix_user_roles_user_id ON user_roles (user_id);
CREATE INDEX ix_user_roles_role_id ON user_roles (role_id);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE RESTRICT,
    CONSTRAINT uq_role_permissions_pair UNIQUE (role_id, permission_id)
);
CREATE INDEX ix_role_permissions_role_id ON role_permissions (role_id);

CREATE TABLE login_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    username_attempt VARCHAR(120) NOT NULL,
    succeeded BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logged_out_at TIMESTAMPTZ,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500)
);
CREATE INDEX ix_login_logs_user_time ON login_logs (user_id, occurred_at DESC);
CREATE INDEX ix_login_logs_outcome_time ON login_logs (succeeded, occurred_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(80) NOT NULL,
    record_id VARCHAR(120),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(64),
    device_info VARCHAR(500),
    old_value TEXT,
    new_value TEXT
);
CREATE INDEX ix_audit_logs_actor_time ON audit_logs (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_logs_module_record ON audit_logs (module, record_id);

INSERT INTO roles (code, name, description, system_role) VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Full system ownership and recovery access.', TRUE),
    ('ADMIN', 'Administrator', 'School administration and delegated configuration.', TRUE),
    ('HEADMASTER', 'Headmaster', 'Academic oversight and school-level approvals.', TRUE),
    ('TEACHER', 'Teacher', 'Assigned-class teaching and assessment workflows.', TRUE),
    ('ACCOUNTANT', 'Accountant', 'Fee account and payment operations.', TRUE),
    ('SECRETARY', 'Secretary', 'Administrative records and admissions intake.', TRUE),
    ('LIBRARIAN', 'Librarian', 'Library catalogue and circulation.', TRUE),
    ('STORE_MANAGER', 'Store Manager', 'Inventory and stock movement.', TRUE),
    ('DATA_ENTRY_OPERATOR', 'Data Entry Operator', 'Delegated data-entry operations.', TRUE),
    ('STUDENT', 'Student', 'Access to own published school information.', TRUE),
    ('PARENT', 'Parent/Guardian', 'Access to linked students approved information.', TRUE);

INSERT INTO permissions (code, description) VALUES
    ('STUDENT_VIEW', 'View and search authorised student records.'),
    ('STUDENT_CREATE', 'Create student records.'),
    ('STUDENT_EDIT', 'Edit permitted student fields.'),
    ('STUDENT_ARCHIVE', 'Archive student records.'),
    ('ADMISSION_REVIEW', 'Review admission applications.'),
    ('ADMISSION_APPROVE', 'Approve or reject admission applications.'),
    ('STAFF_VIEW', 'View staff records.'),
    ('STAFF_MANAGE', 'Create and edit staff records.'),
    ('ACADEMICS_VIEW', 'View academic configuration and enrolments.'),
    ('ACADEMICS_MANAGE', 'Manage academic configuration and enrolments.'),
    ('ATTENDANCE_VIEW', 'View authorised attendance records.'),
    ('ATTENDANCE_RECORD', 'Record attendance for assigned classes.'),
    ('ATTENDANCE_CORRECT', 'Correct attendance with a recorded reason.'),
    ('RESULT_VIEW', 'View authorised results.'),
    ('RESULT_ENTER', 'Enter or edit draft results for assigned subjects.'),
    ('RESULT_APPROVE', 'Approve submitted results.'),
    ('RESULT_LOCK', 'Lock approved results.'),
    ('RESULT_CORRECT_APPROVED', 'Correct approved results through controlled workflow.'),
    ('FINANCE_VIEW', 'View authorised fee accounts and reports.'),
    ('FEE_CHARGE_CREATE', 'Create charges and fee adjustments.'),
    ('PAYMENT_RECORD', 'Record student payments.'),
    ('PAYMENT_REVERSE', 'Reverse payments using controlled workflow.'),
    ('RECEIPT_REPRINT', 'View and reprint an existing receipt.'),
    ('TIMETABLE_MANAGE', 'Manage timetables and conflict overrides.'),
    ('LIBRARY_MANAGE', 'Manage catalogue and circulation.'),
    ('INVENTORY_MANAGE', 'Manage inventory and stock transactions.'),
    ('REPORT_VIEW', 'View authorised operational reports.'),
    ('AUDIT_VIEW', 'View audit and login history.'),
    ('USER_MANAGE', 'Create and manage user accounts.'),
    ('ROLE_MANAGE', 'Manage role definitions and grants.'),
    ('PERMISSION_ASSIGN', 'Assign permissions to roles.'),
    ('SETTINGS_EDIT', 'Edit school and system settings.'),
    ('BACKUP_CREATE', 'Create and inspect database backups.'),
    ('BACKUP_RESTORE', 'Restore a verified backup.');

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code IN (
    'STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT', 'STUDENT_ARCHIVE',
    'ADMISSION_REVIEW', 'ADMISSION_APPROVE', 'STAFF_VIEW', 'STAFF_MANAGE',
    'ACADEMICS_VIEW', 'ACADEMICS_MANAGE', 'ATTENDANCE_VIEW', 'RESULT_VIEW',
    'RESULT_APPROVE', 'RESULT_LOCK', 'FINANCE_VIEW', 'TIMETABLE_MANAGE',
    'LIBRARY_MANAGE', 'INVENTORY_MANAGE', 'REPORT_VIEW', 'AUDIT_VIEW',
    'USER_MANAGE', 'ROLE_MANAGE', 'SETTINGS_EDIT', 'BACKUP_CREATE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'HEADMASTER'
  AND permissions.code IN (
    'STUDENT_VIEW', 'ADMISSION_REVIEW', 'ADMISSION_APPROVE', 'STAFF_VIEW',
    'ACADEMICS_VIEW', 'ACADEMICS_MANAGE', 'ATTENDANCE_VIEW', 'RESULT_VIEW',
    'RESULT_APPROVE', 'RESULT_LOCK', 'FINANCE_VIEW', 'REPORT_VIEW', 'AUDIT_VIEW'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'TEACHER'
  AND permissions.code IN (
    'STUDENT_VIEW', 'ACADEMICS_VIEW', 'ATTENDANCE_VIEW', 'ATTENDANCE_RECORD',
    'ATTENDANCE_CORRECT', 'RESULT_VIEW', 'RESULT_ENTER', 'REPORT_VIEW'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'ACCOUNTANT'
  AND permissions.code IN (
    'STUDENT_VIEW', 'FINANCE_VIEW', 'FEE_CHARGE_CREATE', 'PAYMENT_RECORD',
    'RECEIPT_REPRINT', 'REPORT_VIEW'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'SECRETARY'
  AND permissions.code IN (
    'STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT', 'ADMISSION_REVIEW',
    'STAFF_VIEW', 'ACADEMICS_VIEW', 'ATTENDANCE_VIEW', 'RESULT_VIEW', 'REPORT_VIEW'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'DATA_ENTRY_OPERATOR'
  AND permissions.code IN (
    'STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT', 'STAFF_VIEW', 'STAFF_MANAGE',
    'ACADEMICS_VIEW', 'ATTENDANCE_VIEW', 'RESULT_VIEW', 'RESULT_ENTER'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'LIBRARIAN' AND permissions.code = 'LIBRARY_MANAGE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles CROSS JOIN permissions
WHERE roles.code = 'STORE_MANAGER' AND permissions.code = 'INVENTORY_MANAGE';
