CREATE TABLE assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    assessment_type VARCHAR(40) NOT NULL,
    total_marks NUMERIC(10,2) NOT NULL,
    assessment_date DATE NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assesment_type UNIQUE (subject_id, academic_year_id, school_class_id, assessment_type)
);

CREATE TABLE assessment_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE RESTRICT,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    score NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assessment_score UNIQUE (assessment_id, student_id)
);

CREATE INDEX ix_assessments_subject_class_year_type ON assessments (subject_id, academic_year_id, school_class_id, assessment_type);
CREATE INDEX ix_assessments_date ON assessments (assessment_date);
CREATE INDEX ix_assessment_scores_student ON assessment_scores (student_id);

INSERT INTO permissions (code, description)
SELECT v.code, v.description
FROM (VALUES
    ('ASSESSMENT_VIEW', 'View assessment configuration and published results.'),
    ('ASSESSMENT_WRITE', 'Create and manage assessments and assessment scores.')
) AS v(code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN ('ASSESSMENT_VIEW', 'ASSESSMENT_WRITE')
WHERE roles.code IN ('SUPER_ADMIN', 'ADMIN', 'HEADMASTER', 'TEACHER', 'SECRETARY')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'ASSESSMENT_VIEW'
WHERE roles.code IN ('DATA_ENTRY_OPERATOR')
ON CONFLICT DO NOTHING;
