package com.school.management.academics;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGradeBandRequest(
    @NotBlank @Size(max = 20) String gradeLabel,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minimumPercentage,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal maximumPercentage,
    @Size(max = 300) String remark
) {
}
