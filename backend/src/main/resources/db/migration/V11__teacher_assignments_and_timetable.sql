CREATE TABLE teacher_subject_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    teacher_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_teacher_assignment_class_subject_year UNIQUE (academic_year_id, school_class_id, subject_id)
);

CREATE TABLE timetable_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_subject_assignment_id UUID NOT NULL REFERENCES teacher_subject_assignments(id) ON DELETE RESTRICT,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_timetable_entry_times CHECK (end_time > start_time)
);

CREATE INDEX ix_teacher_assignments_teacher_year ON teacher_subject_assignments (teacher_user_id, academic_year_id);
CREATE INDEX ix_timetable_entries_assignment_day_time ON timetable_entries (teacher_subject_assignment_id, day_of_week, start_time);