package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

public record StudentEnrollmentResponse(
    UUID id,
    String studentAdmissionNumber,
    String academicYearCode,
    String schoolClassCode,
    String classStreamName,
    String status,
    LocalDate enrollmentDate
) {
    public static StudentEnrollmentResponse from(StudentEnrollment enrollment) {
        return new StudentEnrollmentResponse(
            enrollment.getId(),
            enrollment.getStudent().getAdmissionNumber(),
            enrollment.getAcademicYear().getCode(),
            enrollment.getSchoolClass().getCode(),
            enrollment.getClassStream() == null ? null : enrollment.getClassStream().getName(),
            enrollment.getStatus(),
            enrollment.getEnrollmentDate()
        );
    }
}
