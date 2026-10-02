package com.school.management.academics;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record TimetableEntryResponse(
    UUID id,
    UUID assignmentId,
    UUID academicYearId,
    String academicYearCode,
    UUID schoolClassId,
    String schoolClassCode,
    String schoolClassName,
    UUID subjectId,
    String subjectCode,
    String subjectName,
    UUID teacherUserId,
    String teacherName,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String room
) {
    public static TimetableEntryResponse from(TimetableEntry entry) {
        TeacherSubjectAssignment assignment = entry.getAssignment();
        return new TimetableEntryResponse(
            entry.getId(), assignment.getId(),
            assignment.getAcademicYear().getId(), assignment.getAcademicYear().getCode(),
            assignment.getSchoolClass().getId(), assignment.getSchoolClass().getCode(), assignment.getSchoolClass().getName(),
            assignment.getSubject().getId(), assignment.getSubject().getCode(), assignment.getSubject().getName(),
            assignment.getTeacher().getId(), assignment.getTeacher().getDisplayName(),
            entry.getDayOfWeek(), entry.getStartTime(), entry.getEndTime(), entry.getRoom()
        );
    }
}