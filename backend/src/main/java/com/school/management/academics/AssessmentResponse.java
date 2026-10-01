package com.school.management.academics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssessmentResponse(
    UUID id,
    String subjectCode,
    String academicYearCode,
    String schoolClassCode,
    String assessmentType,
    BigDecimal totalMarks,
    LocalDate assessmentDate,
    LocalDate dueDate
) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
            assessment.getId(),
            assessment.getSubject().getCode(),
            assessment.getAcademicYear().getCode(),
            assessment.getSchoolClass().getCode(),
            assessment.getAssessmentType(),
            assessment.getTotalMarks(),
            assessment.getAssessmentDate(),
            assessment.getDueDate()
        );
    }
}
