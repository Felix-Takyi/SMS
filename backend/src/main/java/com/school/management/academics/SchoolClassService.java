package com.school.management.academics;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {
    private final SchoolClassRepository schoolClasses;

    public SchoolClassService(SchoolClassRepository schoolClasses) {
        this.schoolClasses = schoolClasses;
    }

    @Transactional(readOnly = true)
    public Page<SchoolClassResponse> listClasses(Pageable pageable) {
        return schoolClasses.findAll(pageable).map(SchoolClassResponse::from);
    }

    @Transactional
    public SchoolClassResponse createSchoolClass(CreateSchoolClassRequest request) {
        String code = request.code().trim();
        if (schoolClasses.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A class with that code already exists.");
        }

        SchoolClass schoolClass = new SchoolClass(code, request.name(), request.active());
        return SchoolClassResponse.from(schoolClasses.save(schoolClass));
    }

    @Transactional
    public SchoolClassResponse updateSchoolClass(UUID id, CreateSchoolClassRequest request) {
        SchoolClass schoolClass = schoolClasses.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Class was not found."));
        String code = request.code().trim();
        if (schoolClasses.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A class with that code already exists.");
        }
        schoolClass.update(code, request.name(), request.active());
        return SchoolClassResponse.from(schoolClass);
    }
}
