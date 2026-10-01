package com.school.management.academics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.school.management.students.Student;
import com.school.management.students.StudentRepository;

class AttendanceSessionServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final AcademicYearRepository academicYears = mock(AcademicYearRepository.class);
    private final SchoolClassRepository schoolClasses = mock(SchoolClassRepository.class);
    private final AttendanceSessionRepository attendanceSessions = mock(AttendanceSessionRepository.class);
    private final AttendanceRecordRepository attendanceRecords = mock(AttendanceRecordRepository.class);
    private final AttendanceSessionService service = new AttendanceSessionService(
        students, academicYears, schoolClasses, attendanceSessions, attendanceRecords
    );

    @Test
    void createAttendanceSessionRejectsMissingClassOrAcademicYear() {
        UUID academicYearId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();

        when(academicYears.findById(academicYearId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createAttendanceSession(new CreateAttendanceSessionRequest(
                academicYearId,
                schoolClassId,
                LocalDate.now(),
                "Morning"
            )));

        assertEquals("Academic year was not found.", exception.getMessage());
    }

    @Test
    void listAttendanceSessionsReturnsPage() {
        AcademicYear academicYear = new AcademicYear(
            "2026/2027",
            "2026/2027 Academic Year",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            true
        );
        SchoolClass schoolClass = new SchoolClass("JSS1", "Junior Secondary One", true);
        AttendanceSession session = new AttendanceSession(academicYear, schoolClass, LocalDate.now(), "Morning");
        when(attendanceSessions.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(session)));

        Page<AttendanceSessionResponse> page = service.listAttendanceSessions(Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
        assertEquals("Morning", page.getContent().getFirst().sessionName());
    }

    @Test
    void recordAttendanceRejectsUnknownStudent() {
        UUID studentId = UUID.randomUUID();
        when(students.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.recordAttendance(new CreateAttendanceRecordRequest(
                UUID.randomUUID(),
                studentId,
                "PRESENT",
                "on time"
            )));

        assertEquals("Student was not found.", exception.getMessage());
    }
}
