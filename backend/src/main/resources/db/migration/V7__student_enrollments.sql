CREATE TABLE student_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    class_stream_id UUID NULL REFERENCES class_streams(id) ON DELETE RESTRICT,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_student_enrollments_student_year ON student_enrollments (student_id, academic_year_id);
CREATE INDEX ix_student_enrollments_status ON student_enrollments (status);
CREATE INDEX ix_student_enrollments_class ON student_enrollments (school_class_id);
