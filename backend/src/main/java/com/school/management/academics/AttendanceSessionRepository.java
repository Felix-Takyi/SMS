package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
    boolean existsByAcademicYearIdAndSchoolClassIdAndAttendanceDateAndSessionName(UUID academicYearId, UUID schoolClassId, java.time.LocalDate attendanceDate, String sessionName);
}
