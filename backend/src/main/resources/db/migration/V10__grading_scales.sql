CREATE TABLE grading_scales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_grading_scales_name_lower ON grading_scales (LOWER(name));

CREATE TABLE grade_bands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grading_scale_id UUID NOT NULL REFERENCES grading_scales(id) ON DELETE RESTRICT,
    grade_label VARCHAR(20) NOT NULL,
    minimum_percentage NUMERIC(5,2) NOT NULL CHECK (minimum_percentage >= 0 AND minimum_percentage <= 100),
    maximum_percentage NUMERIC(5,2) NOT NULL CHECK (maximum_percentage >= 0 AND maximum_percentage <= 100),
    remark VARCHAR(300),
    CONSTRAINT ck_grade_band_percentage_order CHECK (minimum_percentage <= maximum_percentage),
    CONSTRAINT uq_grade_band_label UNIQUE (grading_scale_id, grade_label)
);
CREATE INDEX ix_grade_bands_scale_minimum ON grade_bands (grading_scale_id, minimum_percentage DESC);
