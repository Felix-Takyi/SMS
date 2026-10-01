CREATE TABLE admission_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_number VARCHAR(80) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    middle_name VARCHAR(120),
    last_name VARCHAR(120) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(30) NOT NULL,
    nationality VARCHAR(120),
    phone VARCHAR(30),
    email VARCHAR(254),
    address VARCHAR(255),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_admission_applications_number UNIQUE (application_number),
    CONSTRAINT chk_admission_applications_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN')),
    CONSTRAINT chk_admission_applications_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WAITLISTED', 'ENROLLED'))
);

CREATE INDEX ix_admission_applications_status ON admission_applications (status);
CREATE INDEX ix_admission_applications_last_name ON admission_applications (last_name);
CREATE INDEX ix_admission_applications_number_lower ON admission_applications (LOWER(application_number));
