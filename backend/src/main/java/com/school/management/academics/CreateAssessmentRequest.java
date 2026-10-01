package com.school.management.academics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssessmentRequest(
    @NotNull UUID subjectId,
    @NotNull UUID academicYearId,
    @NotNull UUID schoolClassId,
    @NotBlank @Size(max = 40) String assessmentType,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal totalMarks,
    @NotNull LocalDate assessmentDate,
    @NotNull LocalDate dueDate
) {
}
