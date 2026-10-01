package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

public record TermResponse(
    UUID id,
    String academicYearCode,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean active
) {
    public static TermResponse from(Term term) {
        return new TermResponse(
            term.getId(),
            term.getAcademicYear().getCode(),
            term.getName(),
            term.getStartDate(),
            term.getEndDate(),
            term.isActive()
        );
    }
}
