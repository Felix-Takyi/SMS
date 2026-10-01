package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingScaleRepository extends JpaRepository<GradingScale, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
