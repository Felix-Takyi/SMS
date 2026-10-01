package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {
    boolean existsByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
}
