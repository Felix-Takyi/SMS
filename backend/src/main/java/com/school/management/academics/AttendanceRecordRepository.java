package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    boolean existsByAttendanceSessionIdAndStudentId(UUID attendanceSessionId, UUID studentId);
}
