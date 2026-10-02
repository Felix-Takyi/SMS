package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {
    boolean existsByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
    Page<AssessmentScore> findByAssessmentId(UUID assessmentId, Pageable pageable);
    long countByAssessmentId(UUID assessmentId);
    void deleteByAssessmentId(UUID assessmentId);
}
