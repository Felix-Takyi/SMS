package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, UUID> {
    boolean existsByAcademicYearIdAndSchoolClassIdAndSubjectId(UUID academicYearId, UUID schoolClassId, UUID subjectId);

    @EntityGraph(attributePaths = {"academicYear", "schoolClass", "subject", "teacher"})
    List<TeacherSubjectAssignment> findAll();
}