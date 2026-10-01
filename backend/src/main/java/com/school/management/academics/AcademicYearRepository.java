package com.school.management.academics;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<AcademicYear> findByCodeIgnoreCase(String code);
}
