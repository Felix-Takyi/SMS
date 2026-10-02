package com.school.management.academics;

import java.util.UUID;

public record TeacherSubjectAssignmentResponse(
    UUID id,
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
    String teacherUsername
) {
    public static TeacherSubjectAssignmentResponse from(TeacherSubjectAssignment assignment) {
        return new TeacherSubjectAssignmentResponse(
            assignment.getId(),
            assignment.getAcademicYear().getId(),
            assignment.getAcademicYear().getCode(),
            assignment.getSchoolClass().getId(),
            assignment.getSchoolClass().getCode(),
            assignment.getSchoolClass().getName(),
            assignment.getSubject().getId(),
            assignment.getSubject().getCode(),
            assignment.getSubject().getName(),
            assignment.getTeacher().getId(),
            assignment.getTeacher().getDisplayName(),
            assignment.getTeacher().getUsername()
        );
    }
}