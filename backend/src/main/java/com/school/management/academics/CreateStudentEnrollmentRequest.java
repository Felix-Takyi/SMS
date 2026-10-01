package com.school.management.academics;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStudentEnrollmentRequest(
    @NotNull UUID studentId,
    @NotNull UUID academicYearId,
    @NotNull UUID schoolClassId,
    UUID classStreamId,
    @NotBlank @Size(max = 40) String status
) {
}
