package com.school.management.academics;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeBandResponse(
    UUID id,
    String gradeLabel,
    BigDecimal minimumPercentage,
    BigDecimal maximumPercentage,
    String remark
) {
    public static GradeBandResponse from(GradeBand band) {
        return new GradeBandResponse(band.getId(), band.getGradeLabel(), band.getMinimumPercentage(),
            band.getMaximumPercentage(), band.getRemark());
    }
}
