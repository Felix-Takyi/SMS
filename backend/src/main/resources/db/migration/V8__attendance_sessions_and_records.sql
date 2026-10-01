CREATE TABLE attendance_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    attendance_date DATE NOT NULL,
    session_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_session UNIQUE (academic_year_id, school_class_id, attendance_date, session_name)
);

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_session_id UUID NOT NULL REFERENCES attendance_sessions(id) ON DELETE RESTRICT,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_record UNIQUE (attendance_session_id, student_id)
);

CREATE INDEX ix_attendance_sessions_date_class ON attendance_sessions (attendance_date, school_class_id);
CREATE INDEX ix_attendance_sessions_academic_year ON attendance_sessions (academic_year_id);
CREATE INDEX ix_attendance_records_status ON attendance_records (status);
