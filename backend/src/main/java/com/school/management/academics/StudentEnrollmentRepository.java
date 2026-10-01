package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    boolean existsByStudentIdAndAcademicYearIdAndStatus(UUID studentId, UUID academicYearId, String status);
}
