CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admission_number VARCHAR(80) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    middle_name VARCHAR(120),
    last_name VARCHAR(120) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(30) NOT NULL,
    nationality VARCHAR(120),
    religion VARCHAR(120),
    phone VARCHAR(30),
    email VARCHAR(254),
    address VARCHAR(255),
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_students_admission_number UNIQUE (admission_number),
    CONSTRAINT chk_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN')),
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'GRADUATED', 'ARCHIVED'))
);

CREATE INDEX ix_students_status ON students (status);
CREATE INDEX ix_students_last_name ON students (last_name);
CREATE INDEX ix_students_admission_number_lower ON students (LOWER(admission_number));
