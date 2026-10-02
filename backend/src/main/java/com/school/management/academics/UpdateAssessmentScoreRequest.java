package com.school.management.academics;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentScoreRequest(
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal score
) {
}
