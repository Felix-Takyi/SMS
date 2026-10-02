package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    boolean existsBySubjectId(UUID subjectId);

    boolean existsBySubjectIdAndAcademicYearIdAndSchoolClassIdAndAssessmentType(
        UUID subjectId, UUID academicYearId, UUID schoolClassId, String assessmentType);
}
