package com.school.management.academics;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentScoreRequest(
    @NotNull UUID assessmentId,
    @NotNull UUID studentId,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal score
) {
}
