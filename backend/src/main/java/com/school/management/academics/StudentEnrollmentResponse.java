package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

public record StudentEnrollmentResponse(
    UUID id,
    UUID studentId,
    String studentAdmissionNumber,
    UUID academicYearId,
    String academicYearCode,
    UUID schoolClassId,
    String schoolClassCode,
    UUID classStreamId,
    String classStreamName,
    String status,
    LocalDate enrollmentDate
) {
    public static StudentEnrollmentResponse from(StudentEnrollment enrollment) {
        return new StudentEnrollmentResponse(
            enrollment.getId(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getAdmissionNumber(),
            enrollment.getAcademicYear().getId(),
            enrollment.getAcademicYear().getCode(),
            enrollment.getSchoolClass().getId(),
            enrollment.getSchoolClass().getCode(),
            enrollment.getClassStream() == null ? null : enrollment.getClassStream().getId(),
            enrollment.getClassStream() == null ? null : enrollment.getClassStream().getName(),
            enrollment.getStatus(),
            enrollment.getEnrollmentDate()
        );
    }
}
