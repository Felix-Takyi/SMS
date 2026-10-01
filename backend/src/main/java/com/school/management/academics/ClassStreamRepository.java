package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassStreamRepository extends JpaRepository<ClassStream, UUID> {
    boolean existsBySchoolClassIdAndNameIgnoreCase(UUID schoolClassId, String name);
    org.springframework.data.domain.Page<ClassStream> findBySchoolClassId(UUID schoolClassId, org.springframework.data.domain.Pageable pageable);
}
