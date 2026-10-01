package com.school.management.academics;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceSessionResponse(
    UUID id,
    String academicYearCode,
    String schoolClassCode,
    LocalDate attendanceDate,
    String sessionName
) {
    public static AttendanceSessionResponse from(AttendanceSession session) {
        return new AttendanceSessionResponse(
            session.getId(),
            session.getAcademicYear().getCode(),
            session.getSchoolClass().getCode(),
            session.getAttendanceDate(),
            session.getSessionName()
        );
    }
}
