package com.school.management.academics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentScoreResponse(
    UUID id,
    UUID assessmentId,
    String assessmentType,
    UUID studentId,
    String studentAdmissionNumber,
    String studentName,
    BigDecimal score,
    BigDecimal totalMarks,
    Instant createdAt,
    Instant updatedAt
) {
    public static AssessmentScoreResponse from(AssessmentScore assessmentScore) {
        var student = assessmentScore.getStudent();
        String name = java.util.stream.Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" "));
        return new AssessmentScoreResponse(
            assessmentScore.getId(),
            assessmentScore.getAssessment().getId(),
            assessmentScore.getAssessment().getAssessmentType(),
            student.getId(),
            student.getAdmissionNumber(),
            name,
            assessmentScore.getScore(),
            assessmentScore.getAssessment().getTotalMarks(),
            assessmentScore.getCreatedAt(),
            assessmentScore.getUpdatedAt()
        );
    }
}
