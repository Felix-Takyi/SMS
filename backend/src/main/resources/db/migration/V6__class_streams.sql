CREATE TABLE class_streams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_streams_school_class_name UNIQUE (school_class_id, name)
);

CREATE INDEX ix_class_streams_school_class_id ON class_streams (school_class_id);
CREATE INDEX ix_class_streams_active ON class_streams (active);
