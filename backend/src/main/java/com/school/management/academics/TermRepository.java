package com.school.management.academics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, UUID> {
    List<Term> findByAcademicYearIdOrderByStartDateAsc(UUID academicYearId);
}
