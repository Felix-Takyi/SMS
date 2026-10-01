package com.school.management.admissions;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, UUID> {
    boolean existsByApplicationNumberIgnoreCase(String applicationNumber);
}
