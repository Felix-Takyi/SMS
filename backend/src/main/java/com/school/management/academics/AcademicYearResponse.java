package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearResponse(
    UUID id,
    String code,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean active
) {
    public static AcademicYearResponse from(AcademicYear academicYear) {
        return new AcademicYearResponse(
            academicYear.getId(),
            academicYear.getCode(),
            academicYear.getName(),
            academicYear.getStartDate(),
            academicYear.getEndDate(),
            academicYear.isActive()
        );
    }
}
