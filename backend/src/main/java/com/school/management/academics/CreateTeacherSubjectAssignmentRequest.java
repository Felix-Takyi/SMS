package com.school.management.academics;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateTeacherSubjectAssignmentRequest(
    @NotNull UUID academicYearId,
    @NotNull UUID schoolClassId,
    @NotNull UUID subjectId,
    @NotNull UUID teacherUserId
) {
}