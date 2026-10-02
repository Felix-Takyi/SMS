package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, UUID> {
    boolean existsByAcademicYearIdAndSchoolClassIdAndSubjectId(UUID academicYearId, UUID schoolClassId, UUID subjectId);
    boolean existsByAcademicYearIdAndSchoolClassIdAndSubjectIdAndTeacher_Id(
        UUID academicYearId, UUID schoolClassId, UUID subjectId, UUID teacherId);

    @Query("select assignment from TeacherSubjectAssignment assignment "
        + "join fetch assignment.academicYear join fetch assignment.schoolClass "
        + "join fetch assignment.subject join fetch assignment.teacher")
    List<TeacherSubjectAssignment> findAllWithRelations();
}