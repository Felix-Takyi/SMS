package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.students.Student;
import com.school.management.students.StudentRepository;

@Service
public class AttendanceSessionService {
    private final StudentRepository students;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final AttendanceSessionRepository attendanceSessions;
    private final AttendanceRecordRepository attendanceRecords;

    public AttendanceSessionService(StudentRepository students,
                                   AcademicYearRepository academicYears,
                                   SchoolClassRepository schoolClasses,
                                   AttendanceSessionRepository attendanceSessions,
                                   AttendanceRecordRepository attendanceRecords) {
        this.students = students;
        this.academicYears = academicYears;
        this.schoolClasses = schoolClasses;
        this.attendanceSessions = attendanceSessions;
        this.attendanceRecords = attendanceRecords;
    }

    @Transactional(readOnly = true)
    public Page<AttendanceSessionResponse> listAttendanceSessions(Pageable pageable) {
        return attendanceSessions.findAll(pageable).map(AttendanceSessionResponse::from);
    }

    @Transactional
    public AttendanceSessionResponse createAttendanceSession(CreateAttendanceSessionRequest request) {
        AcademicYear academicYear = academicYears.findById(request.academicYearId())
            .orElseThrow(() -> new IllegalArgumentException("Academic year was not found."));

        SchoolClass schoolClass = schoolClasses.findById(request.schoolClassId())
            .orElseThrow(() -> new IllegalArgumentException("School class was not found."));

        String sessionName = request.sessionName().trim();
        if (attendanceSessions.existsByAcademicYearIdAndSchoolClassIdAndAttendanceDateAndSessionName(
            academicYear.getId(), schoolClass.getId(), request.attendanceDate(), sessionName)) {
            throw new IllegalArgumentException("An attendance session already exists for that class, date and session name.");
        }

        AttendanceSession attendanceSession = new AttendanceSession(academicYear, schoolClass, request.attendanceDate(), sessionName);
        return AttendanceSessionResponse.from(attendanceSessions.save(attendanceSession));
    }

    @Transactional
    public AttendanceRecord recordAttendance(CreateAttendanceRecordRequest request) {
        Student student = students.findById(request.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student was not found."));

        AttendanceSession attendanceSession = attendanceSessions.findById(request.attendanceSessionId())
            .orElseThrow(() -> new IllegalArgumentException("Attendance session was not found."));

        if (attendanceRecords.existsByAttendanceSessionIdAndStudentId(attendanceSession.getId(), student.getId())) {
            throw new IllegalArgumentException("A record for this student already exists in the attendance session.");
        }

        return attendanceRecords.save(new AttendanceRecord(attendanceSession, student, request.status(), request.note()));
    }
}
